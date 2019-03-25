package com.lzy.down

/**
 * Created by 李志云 2019/3/25 15:03
 */


inline fun Any.download(downloadRequest: DownloadRequest.() -> Unit):DownloadRequest{
    val dr = DownloadUtil.url("").build()
    dr.apply(downloadRequest)
    return dr
}