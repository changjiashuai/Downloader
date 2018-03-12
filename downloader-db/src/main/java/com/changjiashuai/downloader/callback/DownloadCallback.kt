package com.changjiashuai.downloader.callback

import java.io.File

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/9 10:07.
 */
interface DownloadCallback : FileCallback {

    /**
     *开始
     */
    fun onStart(currentSize: Long, totalSize: Long, progress: Float)

    /**
     * 下载中
     */
    fun onProgress(currentSize: Long, totalSize: Long, progress: Float)

    /**
     * 暂停
     */
    fun onPause()

    /**
     * 取消
     */
    fun onCancel()

    /**
     * 完成
     */
    fun onFinish(file: File)

    /**
     * 等待
     */
    fun onWait()

    /**
     * 出错
     */
    fun onError(error: String)
}