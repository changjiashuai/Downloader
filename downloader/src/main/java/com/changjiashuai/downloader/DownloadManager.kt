package com.changjiashuai.downloader

import android.util.Log
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.ForwardingSource
import okio.Okio
import java.io.File
import java.util.*

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/2 15:46.
 */
object DownloadManager {

    private val downCalls = HashMap<String, Call>()
    private val mClient = OkHttpClient.Builder().build()
    lateinit var mDownloadDir: File


    fun download(url: String, downloadObserver: DownloadObserver) {
        Observable.just(url)
            .filter { s -> !downCalls.containsKey(s) }//判断是否在下载中
            .flatMap { s -> Observable.just(createDownloadInfo(s)) }
            .map { getRealFileName(it) }//生成新的文件名
            .flatMap { Observable.create(DownloadSubscribe(it)) } //下载
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(downloadObserver)
    }

    fun cancel(url: String) {
        val call = downCalls[url]
        call?.let { it.cancel() }
        downCalls.remove(url)
    }

    private fun createDownloadInfo(url: String): DownloadInfo {
        val fileName = url.substring(url.lastIndexOf("/") + 1)
        Log.i("DownloadManger", "fileName=$fileName")
        return DownloadInfo(url = url, totalSize = getContentLength(url), fileName = fileName)
    }

    /**
     * 获取下载长度
     */
    private fun getContentLength(url: String): Long {
        val request = Request.Builder()
            .addHeader("Accept-Encoding", "identity")
            .url(url)
            .build()
        return mClient.newCall(request).execute().use {
            if (it.isSuccessful) {
                val contentLength = it.body()?.contentLength()
                Log.i("DownloadManager", "contentLength=$contentLength")
                return@use if (contentLength == -1L) DownloadInfo.TOTAL_ERROR else contentLength!!
            } else DownloadInfo.TOTAL_ERROR
        }
    }

    private fun getRealFileName(downloadInfo: DownloadInfo): DownloadInfo {
        Log.i("DownloadManger", "getRealFileName downloadInfo=$downloadInfo")
        val fileName = downloadInfo.fileName
        var downloadLength = 0L
        val contentLength = downloadInfo.totalSize
        var file = File(mDownloadDir, fileName)
        if (file.exists()) {
            //下载过，获取长度
            downloadLength = file.length()
        }

        //下载过，重新再下一次
        var i = 1
        while (downloadLength >= contentLength) {
            val dotIndex = fileName.lastIndexOf(".")
            val fileNameOther = if (dotIndex == -1) {
                "$fileName($i)"
            } else {
                fileName.substring(0, dotIndex) + "($i)" +
                        fileName.substring(dotIndex)
            }
            val newFile = File(mDownloadDir, fileNameOther)
            file = newFile
            downloadLength = newFile.length()
            i++
        }
        //设置文件名/大小
        downloadInfo.progress = downloadLength
        downloadInfo.fileName = file.name

        Log.i("DownloadManger", "getRealFileName fileName=$fileName")

        return downloadInfo
    }

    private class DownloadSubscribe(val downloadInfo: DownloadInfo) :
        ObservableOnSubscribe<DownloadInfo> {

        override fun subscribe(e: ObservableEmitter<DownloadInfo>) {
            val url = downloadInfo.url
            var downloadLength = downloadInfo.progress //已经下载好的长度
            val contentLength = downloadInfo.totalSize //文件的总长度
            //发送下载信息
            e.onNext(downloadInfo)

            val request = Request.Builder()
                .addHeader(
                    "RANGE",
                    "bytes=$downloadLength-$contentLength"
                ) //确定下载范围，添加此头，则服务器就可以跳过已经下载好的部分, 断点续传的话必须添加这个头
                .url(url)
                .build()
            mClient.newBuilder().addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                return@addNetworkInterceptor originalResponse.newBuilder()
                    .body(ResponseProgressBody(originalResponse.body(), { progress ->
                        Log.i("DownloadManger", "progress=$progress")
                    })).build()
            }
            val call = mClient.newCall(request)
            downCalls[url] = call //记录下载url,方便取消

            val response = call.execute()

            val file = File(mDownloadDir, downloadInfo.fileName)
            Log.i("DownloadManager", "file=${file.name}")

            val sink = Okio.buffer(Okio.sink(file))
            val buffer = sink.buffer()
            var len = 0L
            val bufferSize = 200 * 1024L //200kb

            response.body()?.let {
                val source = it.source()
                source.use {
                    while (it.read(buffer, bufferSize).apply {
                            len = this
                        } != -1L) {
                        sink.emit()
                        downloadLength += len
                        downloadInfo.progress = downloadLength
                        e.onNext(downloadInfo)
                    }
                }
            }
            sink.close()
            downCalls.remove(url)
            e.onComplete()
        }
    }
}