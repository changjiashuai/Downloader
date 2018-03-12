package com.changjiashuai.downloader.ext

import android.text.TextUtils
import okhttp3.Response

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/9 12:02.
 */

/**
 * 服务器文件是否已更改
 */
internal fun Response.isNotServerFileChanged(): Boolean {
    return code() == 206
}

internal fun Response.isSupportRange(): Boolean {
    val headers = headers()
    return !TextUtils.isEmpty(headers.get("Content-Range")) || headers.get("Content-Length")?.toLong() != -1L
}

internal fun Response.lastModify(): String? {
    return headers().get("Last-Modified")
}