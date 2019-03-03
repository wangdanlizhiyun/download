package com.lzy.down

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.lzy.down.SimpleDownloadUtil.mDownloadSizeSp
import com.lzy.down.SimpleDownloadUtil.mIsFinishDownloadSp
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by 李志云 2018/8/22 09:53
 * path:下载到指定目录文件，为空时下载到预置的缓存目录文件夹下
 */
const val WHAT_RESTART = 0
const val WHAT_COMPLETED = 1
const val WHAT_ERROR = 2
const val WHAT_START = 3
const val WHAT_PROGRESS = 4

class DownloadRequest(
    val id: Int, val fromLocalFilePath: String, val url: String,
    val path: String
    , val downloadListener: DownloadListener?
    , val commonDownloadListener: DownloadListener?
) {

    var lastNotifyProcessTime = 0L
    var totalSize = 0L
    var isSupportRange = false
    var lastModifed = ""
    var retryTime = 0

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
        var isFinish = mIsFinishDownloadSp.getBool(getSpKey())
        if (isFinish){
            if (!File(getFilePath()).exists()){
                deleteFile()
                isFinish = false
            }
        }
        return isFinish
    }


    fun deleteFile() {
        if (!TextUtils.isEmpty(path)) {
            try {
                File(getFilePath()).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            SimpleDownloadUtil.mDiskLruCache?.remove(getKey())
        }
        mIsFinishDownloadSp.putBool(getSpKey(), false)
        mDownloadSizeSp.putLong(getSpKey(), 0)
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
        return getKey() + Md5Util.md5(getFilePath())
    }

    fun delayRetry(delay: Long) {
        retryTime = 0
        val msg = mHandler.obtainMessage()
        msg.obj = this
        msg.what = WHAT_RESTART
        mHandler.sendMessageDelayed(msg, delay)
    }

    fun notifyStartDownload() {
        val msg = mHandler.obtainMessage()
        msg.obj = WeakReference(this)
        msg.what = WHAT_START
        mHandler.sendMessage(msg)
    }

    fun notifyProgress(current: Long, total: Long, speed: Long) {
        val msg = mHandler.obtainMessage()
        msg.obj = WeakReference(this)
        val bunlde = Bundle()
        bunlde.putLong("current", current)
        bunlde.putLong("total", total)
        bunlde.putLong("speed", speed)
        msg.data = bunlde
        msg.what = WHAT_PROGRESS
        mHandler.sendMessage(msg)
    }

    fun notifyCompleteDownload() {
        val msg = mHandler.obtainMessage()
        msg.obj = WeakReference(this)
        msg.what = WHAT_COMPLETED
        mHandler.sendMessage(msg)
    }

    fun notifyErrorDownload() {
        val msg = mHandler.obtainMessage()
        msg.obj = WeakReference(this)
        msg.what = WHAT_ERROR
        mHandler.sendMessage(msg)
    }

    fun onStart() {
        retryTime = 0
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
        val mHandler = Handler(Looper.getMainLooper()){msg->
            if (msg.obj is WeakReference<*>) {
                val reference: WeakReference<*> = msg.obj as WeakReference<*>
                reference.get()?.let { request ->
                    if (request is DownloadRequest) {
                        when (msg.what) {
                            WHAT_RESTART -> {
                                if (request.isCancelledCallback.get())  true
                                DownloadUtil.startDownload(request)
                            }
                            else -> {
                                SimpleDownloadUtil.mAllDownloadRequests.forEach {
                                    val request = it.value
                                    if (request.isCancelledCallback.get())  true
                                    if (request.getSpKey() != request.getSpKey()) true
                                    when (msg.what) {
                                        WHAT_START -> request.onStart()
                                        WHAT_COMPLETED -> request.onComplete()
                                        WHAT_ERROR ->
                                            request.onError()
                                        WHAT_PROGRESS -> {
                                            val current = msg.data.getLong("current")
                                            val total = msg.data.getLong("total")
                                            val progress = current * 1.0f / total
                                            val speed = msg.data.getLong("speed")
                                            request.onProgress(progress, speed)
                                        }
                                    }
                                    when(msg.what){
                                        WHAT_COMPLETED, WHAT_ERROR ->SimpleDownloadUtil.mAllDownloadRequests.remove(request.hashCode())
                                    }

                                }

                            }
                        }
                    }
                }
            }
            true
        }
    }
}