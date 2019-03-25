# 一个小巧的下载框架。（支持下载文件自动管理，支持断点续传，支持离线包数据模拟下载。使用了改造后的DiskLruCache结合NIO和断点续传）
 
 
#使用
maven { url 'https://jitpack.io' }
implementation 'com.github.wangdanlizhiyun:download:1.2.1'
 
 初始化
    ```
        DownloadUtil.initParentFile(this, getCacheFile(this, "downloaddemo"), 1024 * 1024 * 1024 * 2L)
    ```
  使用
  
  ```
  //dsl语法调用
  downloadRequest = download {
                              url = "http://wzcdnfs.1089u.com/wz/apk/android/12/helloread_market_v1.3.0.apk"
                              downloadListener = object : DownloadListenerAdapter() {
                                  override fun onProgress(
                                      downloadRequest: DownloadRequest,
                                      url: String,
                                      path: String,
                                      progress: Float,
                                      speed: Long
                                  ) {
                                      super.onProgress(downloadRequest, url, path, progress, speed)
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
                                      super.onComplete(downloadRequest, url, path)
                                      Log.e("test", "${Thread.currentThread().name} onComplete")
                                      seekBar.progress = 100
                                      val intent = Intent(Intent.ACTION_VIEW)
                                      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                      FileProvider7.setIntentDataAndType(
                                          context,
                                          intent, "application/vnd.android.package-archive", File(path), true
                                      );
                                      context.startActivity(intent)
                                  }
                              }
                              commonDownloadListener = null
  
                          }
                          
                          
                    //可以设置全局监听和单独的监听
                    //初始化下载请求,以下载地址和保存地址作为唯一下载任务id
                    downloadRequest =
                                  DownloadUtil.url("http://es-public.oss-cn-shenzhen.aliyuncs.com/dev/180817/2a92799901954ad795d566ebec8860c8.avi.mp4")
                                      .listener(object : DownloadListener {
                                          override fun onProgress(
                                              downloadRequest: DownloadRequest,
                                              url: String,
                                              path: String,
                                              progress: Float, speed: Long
                                          ) {
                                              Log.e("test", "进度:$progress")
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
                                              seekBar.progress = 100
                                          }
  
                                          override fun onStart(
                                              downloadRequest: DownloadRequest,
                                              url: String,
                                              path: String
                                          ) {
                                              mStartTime = System.currentTimeMillis()
                                          }
  
                                          override fun onError(
                                              downloadRequest: DownloadRequest,
                                              url: String,
                                              path: String
                                          ) {
  //                                    downloadRequest.delayRetry(1000)
                                          }
  
                                      }).build()
                      }
                      //开始下载
                      DownloadUtil.startDownload(downloadRequest)
                      //取消下载
                    downloadRequest?.cancelDownload()
                    //恢复回掉
                    downloadRequest?.continueCallback()
                    //取消回掉
                    downloadRequest?.cancelCallback()
                    
                    //特殊情况下需要用本地离线数据模拟网络下载
                    DownloadRequestBuilder().fromLocalFilePath(fromLocalFilePath).url(url).commonListener(object : DownloadStateListener {
                                override fun onComplete(downloadRequest: DownloadRequest,  url: String, path: String) {
                                    MyApplication.sInstance.mDownloadStateSp.putString(url,path)
                                    Log.e("test", "下载完成一个 $ ${url}")
                                    if (downloadRequest.id > 0) {
                    //                    ReportUtil.reportAdDownloadingStatus(ReportUtil.STATUS_FILE_DOWNLOAD_COMPLETE, downloadRequest.id)
                    
                                    }
                                }
                    
                                override fun onStart(downloadRequest: DownloadRequest, url: String, path: String) {
                    //                Log.e("test", "下载开始一个${url}")
                                    if (downloadRequest.id > 0) {
                    //                    ReportUtil.reportAdDownloadingStatus(ReportUtil.STATUS_FILE_DOWNLOADING, downloadRequest.id)
                                    }
                                }
                    
                                override fun onError(downloadRequest: DownloadRequest, url: String, path: String) {
                                    Log.e("test", "下载失败一个${url}")
                                    if (downloadRequest.id > 0) {
                    //                    ReportUtil.reportAdDownloadingStatus(ReportUtil.STATUS_FILE_DOWNLOAD_FAIL, downloadRequest.id)
                                    }
                                }
                    
                            })
                            
                            
    
  ```