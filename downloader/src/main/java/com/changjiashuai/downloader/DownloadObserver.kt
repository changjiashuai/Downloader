package com.changjiashuai.downloader

import io.reactivex.Observer
import io.reactivex.disposables.Disposable

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/6 14:10.
 */
abstract class DownloadObserver : Observer<DownloadInfo> {

    protected lateinit var d: Disposable
    protected lateinit var downloadInfo: DownloadInfo

    override fun onSubscribe(d: Disposable) {
        this.d = d
    }

    override fun onNext(downloadInfo: DownloadInfo) {
        this.downloadInfo = downloadInfo
    }

    override fun onError(e: Throwable) {
        e.printStackTrace()
    }
}