package com.lzy.down

/**
 * Created by 李志云 2019/1/7 00:08
 */
abstract class DownloadListenerAdapter:DownloadListener {
    override fun onComplete(downloadRequest: DownloadRequest, url: String, path: String) {

    }

    override fun onError(downloadRequest: DownloadRequest, url: String, path: String) {

    }

    override fun onPrepare(downloadRequest: DownloadRequest, url: String, path: String) {

    }

    override fun onProgress(downloadRequest: DownloadRequest, url: String, path: String, progress: Float, speed: Long) {

    }

    override fun onStart(downloadRequest: DownloadRequest, url: String, path: String) {

    }
    override fun onCancel(downloadRequest: DownloadRequest, url: String, path: String) {

    }
}