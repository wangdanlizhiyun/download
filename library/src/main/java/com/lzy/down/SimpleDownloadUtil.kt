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
            Log.e("test","onComplete")
            return
        }
        downloadRequest.onPrepare()
        mAllDownloadRequests[downloadRequest.hashCode()] = downloadRequest
        //区分为指定路径和不指定。不指定的自动管理总文件大小。url区分为网络来源和文件来源
        mPool.execute {
            if (mDownloadingRequests.containsKey(downloadRequest.getSpKey())) return@execute
            mDownloadingRequests.put(downloadRequest.getSpKey(), downloadRequest)
            Log.e("test","开始下载")
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
                                val randomAccessFile = edit.newOutputRandomAccessFile(0)
                                downloadUrlToStream(downloadRequest, randomAccessFile)
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
                val randomAccessFile = RandomAccessFile(downloadRequest.path, "rwd")
                downloadUrlToStream(downloadRequest, randomAccessFile)
            }


            mDownloadingRequests.remove(downloadRequest.getSpKey())
            removeDownload(downloadRequest)
        }
    }

    private fun downloadUrlToStream(downloadRequest: DownloadRequest, randomAccessFile: RandomAccessFile) {
        if (TextUtils.isEmpty(downloadRequest.url) && TextUtils.isEmpty(downloadRequest.fromLocalFilePath)) return
        var urlConnection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var fileChannel: FileChannel? = null
        var current = 0L
        var lastCurrent = 0L
        var speed = 0L
        try {
            val file = File(downloadRequest.fromLocalFilePath)
            if (file.exists()) {
                inputStream = FileInputStream(file)
                downloadRequest.totalSize = inputStream.available().toLong()
            } else {

                val start = System.currentTimeMillis()
                val url = URL(downloadRequest.url)
                urlConnection = url.openConnection() as HttpURLConnection
                try {
                    downloadRequest.totalSize = java.lang.Long.parseLong(urlConnection.getHeaderField("content-length"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                inputStream = urlConnection.inputStream
                Log.e("test", "链接耗时${System.currentTimeMillis() - start}")
            }

            current = mDownloadSizeSp.getLong(downloadRequest.getSpKey())
            lastCurrent = current
            Log.e("test", "上次的下载进度=$current")

            randomAccessFile.setLength(downloadRequest.totalSize)
            fileChannel = randomAccessFile.channel
            inputStream?.let {

                val skip = it.skip(current)
                Log.e("test", "skip=$skip")

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
                    mIsFinishDownloadSp.putBool(downloadRequest.getSpKey(), true)
                    downloadRequest.notifyCompleteDownload()
                } else {
                    downloadRequest.notifyErrorDownload()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("test", "downloadUrlToStream catch e=" + e.message)
        } finally {
            Log.e("test", "current=$current totalSize=${downloadRequest.totalSize}")
            try {
                Util.close(inputStream, fileChannel, randomAccessFile)
                urlConnection?.disconnect()
            } catch (e: Exception) {
            }

        }
    }
}
