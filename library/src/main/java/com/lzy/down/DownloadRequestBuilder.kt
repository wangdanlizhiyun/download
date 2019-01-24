package com.lzy.down


/**
 * Created by 李志云 2019/1/7 00:02
 */
class DownloadRequestBuilder {
    private var id = 0
    private var url = ""
    private var fromLocalFilePath = ""
    private var md5: String = ""
    private var path: String = ""
    private var downloadListener: DownloadListener? = null
    private var commonDownloadListener: DownloadListener? = null

    fun id(id: Int): DownloadRequestBuilder {
        this.id = id
        return this
    }

    fun fromLocalFilePath(fromLocalFilePath: String): DownloadRequestBuilder {
        this.fromLocalFilePath = fromLocalFilePath
        return this
    }

    fun md5(md5: String): DownloadRequestBuilder {
        this.md5 = md5
        return this
    }



    fun path(path: String): DownloadRequestBuilder {
        this.path = path
        return this
    }

    fun url(url: String): DownloadRequestBuilder {
        this.url = url
        return this
    }

    fun listener(downloadStateListener: DownloadListener?): DownloadRequestBuilder {
        this.downloadListener = downloadStateListener
        return this
    }

    fun commonListener(downloadListener: DownloadListener?): DownloadRequestBuilder {
        this.commonDownloadListener = downloadListener
        return this
    }

    fun build(): DownloadRequest {
        return DownloadRequest(id, fromLocalFilePath, url, path,md5, downloadListener, commonDownloadListener)
    }

    fun start(): DownloadRequest {
        val downloadRequest = build()
        DownloadUtil.startDownload(downloadRequest)
        return downloadRequest
    }


}