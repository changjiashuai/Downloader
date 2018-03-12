package com.changjiashuai.downloader

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/7 16:55.
 */
class ResponseProgressBody(
    val responseBody: ResponseBody?,
    val progressListener: ((Progress) -> Unit)?
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentLength(): Long {
        return responseBody?.contentLength() ?: 0
    }

    override fun contentType(): MediaType? {
        return responseBody?.contentType()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody?.source()))
        }
        return bufferedSource!!
    }

    private fun source(source: Source?) = object : ForwardingSource(source) {
        var totalBytesRead: Long = 0L
        override fun read(sink: Buffer?, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            totalBytesRead += if (bytesRead != -1L) bytesRead else 0L
            progressListener?.invoke(Progress(totalBytesRead, responseBody?.contentLength() ?: -1))
            return bytesRead
        }
    }
}