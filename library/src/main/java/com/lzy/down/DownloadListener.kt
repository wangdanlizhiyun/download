package com.lzy.down

/**
 * Created by 李志云 2019/1/7 00:08
 */
interface DownloadListener {
    fun onComplete(downloadRequest: DownloadRequest, url: String, path: String)
    fun onStart(downloadRequest: DownloadRequest, url: String, path: String)
    fun onError(downloadRequest: DownloadRequest, url: String, path: String)
    fun onProgress(downloadRequest: DownloadRequest, url: String, path: String, progress:Float,speed:Long)
}