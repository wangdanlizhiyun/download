package com.lzy.down

import android.content.Context
import java.io.File

object DownloadUtil {
    lateinit var parentFile: File


    fun initParentFile(context: Context, file: File,diskCacheSize:Long) {
        parentFile = file
        SimpleDownloadUtil.initParentFile(context.applicationContext,parentFile,diskCacheSize)
    }

    fun url(url: String): DownloadRequestBuilder {
        return DownloadRequestBuilder().url(url)
    }

    fun startDownload(downloadRequest: DownloadRequest?) {
        downloadRequest?.let {
            it.continueDownload()
            SimpleDownloadUtil.startDownload(it)
        }
    }


}
