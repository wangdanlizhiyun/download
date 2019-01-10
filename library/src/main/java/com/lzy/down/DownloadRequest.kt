package com.lzy.down

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import com.lzy.down.SimpleDownloadUtil.downloadSizeSp
import com.lzy.down.SimpleDownloadUtil.isFinishDownloadSp
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by 李志云 2018/8/22 09:53
 * path:下载到指定目录文件，为空时下载到预置的缓存目录文件夹下
 */
class DownloadRequest(
    val id: Int, val fromLocalFilePath: String, val url: String,
    val path: String
    , val downloadListener: DownloadListener?
    , val commonDownloadListener: DownloadListener?
) {
    var lastNotifyProcessTime = 0L
    var totalSize = 0L

    val isCancelledCallback = AtomicBoolean(false)
    val isCancelledDownload = AtomicBoolean(false)
    fun cancelDownload() {
        SimpleDownloadUtil.removeDownload(this)
    }

    fun continueDownload() {
        isCancelledDownload.compareAndSet(true, false)
    }

    fun cancelCallback() {
        isCancelledCallback.compareAndSet(false, true)
    }

    fun continueCallback() {
        isCancelledCallback.compareAndSet(true, false)
    }

    fun hasDownloaded(): Boolean {
        return (!TextUtils.isEmpty(getFilePath())) && File(getFilePath()).exists() && File(getFilePath()).length() > 0 && isFinishDownloadSp.getBool(
            getSpKey()
        )
    }

    fun isNeedDeleteFile(): Boolean {
        return getFilePath().isEmpty() || !File(getFilePath()).exists() || File(getFilePath()).length() == 0L
    }

    fun deleteFile() {
        isFinishDownloadSp.putBool(getSpKey(), false)
        downloadSizeSp.putLong(getSpKey(), 0)
        if (!TextUtils.isEmpty(getFilePath())) {
            try {
                File(getFilePath()).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getFilePath(): String {
        if (!TextUtils.isEmpty(path)) {
            return path
        } else {
            return DownloadUtil.parentFile.absolutePath + File.separator + getKey() + ".0"
        }

    }

    fun getKey(): String {
        return Md5Util.md5(url)
    }

    fun getSpKey(): String {
        return Md5Util.md5(url + "_" + getFilePath())
    }

    fun getFileNameFromUrl(): String {
        try {
            return url.substring(url.lastIndexOf('/') + 1)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return Md5Util.md5(url)
    }

    fun delayRetry(delay: Long) {
        val msg = sHandler.obtainMessage()
        msg.obj = this
        msg.what = WHAT_RESTART
        sHandler.sendMessageDelayed(msg, delay)
    }

    fun notifyStartDownload() {
        val msg = sHandler.obtainMessage()
        msg.obj = WeakReference(this)
        msg.what = WHAT_START
        sHandler.sendMessage(msg)
    }

    fun notifyProgress(current: Long, total: Long, speed: Long) {
        val msg = sHandler.obtainMessage()
        msg.obj = WeakReference(this)
        val bunlde: Bundle = Bundle()
        bunlde.putLong("current", current)
        bunlde.putLong("total", total)
        bunlde.putLong("speed", speed)
        msg.data = bunlde
        msg.what = WHAT_PROGRESS
        sHandler.sendMessage(msg)
    }

    fun notifyCompleteDownload() {
        val msg = sHandler.obtainMessage()
        msg.obj = WeakReference(this)
        msg.what = WHAT_COMPLETED
        sHandler.sendMessage(msg)
    }

    fun notifyErrorDownload() {
        val msg = sHandler.obtainMessage()
        msg.obj = WeakReference(this)
        msg.what = WHAT_ERROR
        sHandler.sendMessage(msg)
    }

    fun onStart() {
        commonDownloadListener?.onStart(this, url, getFilePath())
        downloadListener?.onStart(this, url, getFilePath())
    }

    fun onComplete() {
        commonDownloadListener?.onComplete(this, url, getFilePath())
        downloadListener?.onComplete(this, url, getFilePath())
    }

    fun onPrepare() {
        commonDownloadListener?.onPrepare(this, url, getFilePath())
        downloadListener?.onPrepare(this, url, getFilePath())
    }

    fun onError() {
        commonDownloadListener?.onError(this, url, getFilePath())
        downloadListener?.onError(this, url, getFilePath())
    }

    fun onProgress(progress: Float, speed: Long) {
        commonDownloadListener?.onProgress(this, url, getFilePath(), progress, speed)
        downloadListener?.onProgress(this, url, getFilePath(), progress, speed)
    }


    companion object {
        const val WHAT_RESTART = 0
        const val WHAT_COMPLETED = 1
        const val WHAT_ERROR = 2
        const val WHAT_START = 3
        const val WHAT_PROGRESS = 4
        val sHandler =
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    if (msg.obj is WeakReference<*>) {
                        val reference: WeakReference<*> = msg.obj as WeakReference<*>
                        reference.get()?.let { request ->
                            if (request is DownloadRequest) {
                                when (msg.what) {
                                    WHAT_RESTART -> DownloadUtil.startDownload(request)
                                    else -> {
                                        SimpleDownloadUtil.mAllDownloadRequests.forEach {
                                            val request = it.value
                                            if (request.isCancelledCallback.get()) return
                                            if (request.getSpKey() != request.getSpKey()) return
                                            when (msg.what) {
                                                WHAT_START -> request.onStart()
                                                WHAT_COMPLETED -> request.onComplete()
                                                WHAT_ERROR -> request.onError()
                                                WHAT_PROGRESS -> {
                                                    val current = msg.data.getLong("current")
                                                    val total = msg.data.getLong("total")
                                                    val progress = current * 1.0f / total
                                                    val speed = msg.data.getLong("speed")
                                                    request.onProgress(progress, speed)
                                                }
                                            }

                                        }

                                    }
                                }
                            }
                        }
                    }

                }
            }
    }
}