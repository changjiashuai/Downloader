package com.changjiashuai.downloader

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/6 14:06.
 */
data class DownloadInfo(
    var url: String,
    var totalSize: Long = 0L, //文件的总大小
    var progress: Long = 0L, //当前下载的进度
    var fileName: String = ""
) {
    companion object {
        const val TOTAL_ERROR = -1L
    }
}