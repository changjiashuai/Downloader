package com.changjiashuai.downloader.net

import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/9 11:54.
 */
object OkHttpManager {

    private val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)

    fun initRequest(url: String, start: Long, end: Long, callback: Callback): Call {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-$end")
            .build()
        val call = builder.build().newCall(request)
        call.enqueue(callback)
        return call
    }

    fun initRequest(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-")
            .build()
        return builder.build().newCall(request).execute()
    }

    fun initRequest(url: String, lastModify: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-")
            .header("If-Range", lastModify)
            .build()
        return builder.build().newCall(request).execute()
    }
}