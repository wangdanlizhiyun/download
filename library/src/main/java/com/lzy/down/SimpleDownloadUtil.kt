package com.lzy.down

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.lzy.down.sp.Sp
import com.lzy.down.sp.SpManager
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors


/**
 * Created by 李志云 2018/8/21 14:54
 */
object SimpleDownloadUtil {
    var mDiskLruCache: DiskLruCache? = null
    lateinit var mDownloadSizeSp: Sp
    lateinit var mIsFinishDownloadSp: Sp

    private val mPool = Executors.newFixedThreadPool(4)
    fun initParentFile(context: Context, parentFile: File, diskCacheSize: Long) {
        mDownloadSizeSp = SpManager.getDefault(context).getSp("downloadSize")
        mIsFinishDownloadSp = SpManager.getDefault(context).getSp("isFinishDownloadSp")
        mDiskLruCache = DiskLruCache.open(parentFile, 1, 1, diskCacheSize).setEntryRemovedListener {
            val spKey = it + Md5Util.md5(DownloadUtil.parentFile.absolutePath + File.separator + it + ".0")
            mIsFinishDownloadSp.putBool(spKey, false)
            mDownloadSizeSp.putLong(spKey, 0)
        }
    }

    val mDownloadingRequests = ConcurrentHashMap<String, DownloadRequest>()

    val mAllDownloadRequests = ConcurrentHashMap<Int, DownloadRequest>()

    fun removeDownload(downloadRequest: DownloadRequest) {
        SimpleDownloadUtil.mDownloadingRequests.forEach {
            val request = it.value
            if (request.getSpKey() == downloadRequest.getSpKey()) {
                request.isCancelledDownload.compareAndSet(false, true)
            }
        }
        mAllDownloadRequests.forEach {
            if (downloadRequest.getSpKey() == it.value.getSpKey()) {
                mAllDownloadRequests.remove(it.key)
            }
        }
    }

    fun startDownload(downloadRequest: DownloadRequest) {
        if (downloadRequest.hasDownloaded()) {
            downloadRequest.onComplete()
            return
        }
        //区分为指定路径和不指定。不指定的自动管理总文件大小。url区分为网络来源和文件来源
        mPool.execute {
            if (mDownloadingRequests.containsKey(downloadRequest.getSpKey())) return@execute
            mAllDownloadRequests[downloadRequest.hashCode()] = downloadRequest
            downloadRequest.onPrepare()
            mDownloadingRequests.put(downloadRequest.getSpKey(), downloadRequest)
            val key = downloadRequest.getKey()
            if (TextUtils.isEmpty(downloadRequest.path)) {
                //下载到缓存目录
                mDiskLruCache?.let { disk ->
                    downloadRequest.notifyStartDownload()
                    val snapshot = disk.get(key)
                    snapshot.let {
                        try {
                            val editor = disk.edit(key)
                            editor?.let { edit ->
                                val file = edit.newOutputFile(0)
                                downloadUrlToFile(downloadRequest, file)
                                edit.commit()
                            }
                            try {
                                disk.flush()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        } catch (e1: IOException) {
                            e1.printStackTrace()
                        }
                    }
                }
            } else {
                //下载到指定目录文件
                downloadUrlToFile(downloadRequest, File(downloadRequest.path))
            }
            mDownloadingRequests.remove(downloadRequest.getSpKey())
        }
    }

    private fun downloadUrlToFile(downloadRequest: DownloadRequest, outFile: File) {
        if (TextUtils.isEmpty(downloadRequest.url) && TextUtils.isEmpty(downloadRequest.fromLocalFilePath)) return
        val fromFile = File(downloadRequest.fromLocalFilePath)
        if (fromFile.exists()) {//本地文件拷贝模拟网络下载，一般用于离线包功能
            Util.nioMappedCopy(fromFile, outFile)
            mIsFinishDownloadSp.putBool(downloadRequest.getSpKey(), true)
            downloadRequest.notifyCompleteDownload()
            return
        }

        var isSuccess = false
        var urlConnection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var fileChannel: FileChannel? = null
        var current = mDownloadSizeSp.getLong(downloadRequest.getSpKey())
        var lastCurrent = current
        var speed = 0L
        val randomAccessFile = RandomAccessFile(outFile.absolutePath, "rwd")
        try {
            checkSupportRange(downloadRequest)
            val start = System.currentTimeMillis()
            val url = URL(downloadRequest.url)
            urlConnection = url.openConnection() as HttpURLConnection
            if (downloadRequest.isSupportRange) {
                urlConnection.setRequestProperty("Range", "bytes=$current-${downloadRequest.totalSize}")
                urlConnection.setRequestProperty("If-Range", "${downloadRequest.lastModifed}")
            }
            urlConnection.connectTimeout = 10_000
            urlConnection.readTimeout = 10_000
            urlConnection.connect()
            if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = urlConnection.inputStream
                downloadRequest.deleteFile()
                current = mDownloadSizeSp.getLong(downloadRequest.getSpKey())
                lastCurrent = current
            } else if (urlConnection.responseCode == HttpURLConnection.HTTP_PARTIAL) {
                inputStream = urlConnection.inputStream
            }
            randomAccessFile.setLength(downloadRequest.totalSize)
            fileChannel = randomAccessFile.channel
            inputStream?.let {
                val mappedByteBuffer =
                    fileChannel.map(FileChannel.MapMode.READ_WRITE, current, downloadRequest.totalSize - current)
                val bytes = ByteArray(1024)
                var len: Int
                do {
                    len = it.read(bytes)
                    if (len > 0) {
                        current += len.toLong()
                        mappedByteBuffer.put(bytes, 0, len)
                        mDownloadSizeSp.putLong(downloadRequest.getSpKey(), current)
                    }
                    if ((len > 0 && System.currentTimeMillis() - downloadRequest.lastNotifyProcessTime > 200) || len <= 0) {
                        speed = (current - lastCurrent) * 1000 / 200
                        downloadRequest.lastNotifyProcessTime = System.currentTimeMillis()
                        lastCurrent = current
                        downloadRequest.notifyProgress(current, downloadRequest.totalSize, speed)
                    }
                } while (len != -1 && !downloadRequest.isCancelledDownload.get())
                if (len == -1 && current >= downloadRequest.totalSize) {
                    isSuccess = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                Util.close(inputStream, fileChannel, randomAccessFile)
                urlConnection?.disconnect()
            } catch (e: Exception) {
            }
        }
        if (isSuccess){
            mIsFinishDownloadSp.putBool(downloadRequest.getSpKey(), true)
            downloadRequest.notifyCompleteDownload()
        }else{
            if (downloadRequest.isCancelledDownload.get()){
                downloadRequest.notifyCancelDownload()
            }else{
                if (downloadRequest.retryTime < 4){
                    downloadRequest.retryTime++
                    try {
                        Thread.sleep(7_000)
                    }catch (e:java.lang.Exception){}
                    Log.e("test","重试 ${downloadRequest.retryTime}")
                    downloadUrlToFile(downloadRequest,outFile)
                }else{
                    downloadRequest.notifyErrorDownload()
                }
            }
        }
    }

    private fun checkSupportRange(downloadRequest: DownloadRequest) {
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(downloadRequest.url)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connectTimeout = 10_000
            if (urlConnection.responseCode == 200) {
                downloadRequest.lastModifed = urlConnection.getHeaderField("Last-Modified")
                try {
                    downloadRequest.totalSize = java.lang.Long.parseLong(urlConnection.getHeaderField("content-length"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val ranges = urlConnection.getHeaderField("Accept-Ranges")
                if (ranges == "bytes") {
                    downloadRequest.isSupportRange = true
                }
            }
        } catch (e: Exception) {

        } finally {
            urlConnection?.disconnect()
        }

    }

}
