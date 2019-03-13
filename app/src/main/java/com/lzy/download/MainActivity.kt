package com.lzy.download

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.lzy.down.*
import com.zhy.base.fileprovider.FileProvider7
import org.jetbrains.anko.button
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.seekBar
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import java.io.File

class MainActivity : Activity() {

    var downloadRequest: DownloadRequest? = null
    var mStartTime = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DownloadUtil.initParentFile(this, getCacheFile(this, "downloaddemo"), 1024 * 1024 * 1024 * 2L)
        verticalLayout {
            val seekBar = seekBar {

            }
            val tv = textView {

            }
            button("开始下载") {
                onClick {
                            if (downloadRequest == null) {
                                downloadRequest =
                                    DownloadUtil.url("http://wzcdnfs.1089u.com/wz/apk/android/12/helloread_market_v1.3.0.apk")
                                        .listener(object:DownloadListenerAdapter() {
                                            override fun onCancel(
                                                downloadRequest: DownloadRequest,
                                                url: String,
                                                path: String
                                            ) {

                                                Log.e("test","${Thread.currentThread().name} onCancel")
                                            }

                                            override fun onProgress(
                                                downloadRequest: DownloadRequest,
                                                url: String,
                                                path: String,
                                                progress: Float, speed: Long
                                            ) {
                                                Log.e("test", "${Thread.currentThread().name} 进度:$progress")
                                                tv.text = "进度:$progress  速度:${Util.getFileSize(speed)}/s 文件大小${Util.getFileSize(
                                                    downloadRequest.totalSize
                                                )}" +
                                                        "  耗时：${System.currentTimeMillis() - mStartTime} 毫秒"
                                                seekBar.progress = (progress * 100).toInt()
                                            }

                                            override fun onComplete(
                                                downloadRequest: DownloadRequest,
                                                url: String,
                                                path: String
                                            ) {

                                                Log.e("test","${Thread.currentThread().name} onComplete")
                                                seekBar.progress = 100

                                                val intent = Intent(Intent.ACTION_VIEW)
                                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                FileProvider7.setIntentDataAndType(context,
                                                    intent, "application/vnd.android.package-archive", File(path), true);
                                                context.startActivity(intent)
                                            }

                                            override fun onStart(
                                                downloadRequest: DownloadRequest,
                                                url: String,
                                                path: String
                                            ) {
                                                Log.e("test","${Thread.currentThread().name} onStart")
                                                mStartTime = System.currentTimeMillis()
                                            }

                                            override fun onError(
                                                downloadRequest: DownloadRequest,
                                                url: String,
                                                path: String
                                            ) {
                                                Log.e("test","${Thread.currentThread().name} onError")
                                                //                                    downloadRequest.delayRetry(1000)
                                            }

                                        }).build()
                            }

                            DownloadUtil.startDownload(downloadRequest)

                }
            }

            button("停止下载") {
                onClick {
                    downloadRequest?.cancelDownload()
                }
            }
            button("删除文件") {
                onClick {
                    downloadRequest?.deleteFile()
                    tv.text = "下载进度:0  下载速度:${Util.getFileSize(0)}/s "
                    seekBar.progress = 0
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        downloadRequest?.continueCallback()
    }

    override fun onStop() {
        super.onStop()
        downloadRequest?.cancelCallback()
    }

    fun getCacheFile(context: Context, uniqueName: String): File {
        var cachePath: String? = null
        if (Environment.MEDIA_MOUNTED == Environment
                .getExternalStorageState()
        ) {
            val cacheDir = context.externalCacheDir
            if (cacheDir != null) {
                cachePath = cacheDir.path
            }
        } else {
            cachePath = context.cacheDir.path
        }
        if (cachePath == null) {
            cachePath = Environment.getExternalStorageDirectory().absolutePath
        }
        val file = File(cachePath + File.separator + uniqueName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

}
