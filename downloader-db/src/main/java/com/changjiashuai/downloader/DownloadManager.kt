package com.changjiashuai.downloader

import android.content.Context
import android.util.Log
import com.changjiashuai.downloader.callback.DownloadCallback
import com.changjiashuai.downloader.model.DownloadInfo
import com.changjiashuai.downloader.model.Status

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/8 17:29.
 */
class DownloadManager private constructor(private val context: Context) {

    private val downloadInfoMap = hashMapOf<String, DownloadInfo>()
    private val downloadCallbackMap = hashMapOf<String, DownloadCallback>()
    private val downloadTaskMap = hashMapOf<String, DownloadTask>()
    private lateinit var downloadInfo: DownloadInfo

    companion object : SingletonHolder<DownloadManager, Context>(::DownloadManager)

    @Synchronized
    fun init(url: String, path: String, name: String, childTaskCount: Int = 0) {
        downloadInfo =
                DownloadInfo(url = url, name = name, path = path, childTaskCount = childTaskCount)
    }

    @Synchronized
    private fun execute(downloadInfo: DownloadInfo, downloadCallback: DownloadCallback) {
        if (downloadInfo.childTaskCount == 0) {
            downloadInfo.childTaskCount = 1
        }

        val downloadTask = DownloadTask(context, downloadInfo)
        downloadTask.downloadCallback = downloadCallback

        downloadInfoMap[downloadInfo.url!!] = downloadInfo
        downloadCallbackMap[downloadInfo.url!!] = downloadCallback
        downloadTaskMap[downloadInfo.url!!] = downloadTask

        ThreadPool.threadPoolExecutor.execute(downloadTask)

        //如果正在下载的任务数量等于线程池的核心线程数，则新添加的任务处于等待状态
        if (ThreadPool.threadPoolExecutor.activeCount == ThreadPool.corePoolSize) {
            downloadCallback.onWait()
        }
    }

    fun start(downloadCallback: DownloadCallback) {
        execute(downloadInfo, downloadCallback)
    }

    fun start(downloadInfo: DownloadInfo, downloadCallback: DownloadCallback) {
        execute(downloadInfo, downloadCallback)
    }

    fun start(url: String) {
        execute(downloadInfoMap[url]!!, downloadCallbackMap[url]!!)
    }

    fun registerDownlocaCallback(downloadInfo: DownloadInfo, downloadCallback: DownloadCallback) {
        downloadInfoMap[downloadInfo.url!!] = downloadInfo
        downloadCallbackMap[downloadInfo.url!!] = downloadCallback
    }

    fun pause(url: String) {
        if (downloadTaskMap.containsKey(url)) {
            downloadTaskMap[url]?.pause()
            Log.i("DownloadManager", "pause() status=${downloadTaskMap[url]?.mCurrentState}, url=$url")
        }
    }

    fun resume(url: String) {
        if (downloadTaskMap.containsKey(url)) {
            downloadTaskMap[url]?.let {
                Log.i("DownloadManager", "resume() status=${it.mCurrentState}, url=$url")
                if (it.mCurrentState == Status.PAUSE || it.mCurrentState == Status.ERROR) {
                    downloadTaskMap.remove(url)
                    execute(downloadInfoMap[url]!!, downloadCallbackMap[url]!!)
                }
            }
        }
    }

    fun cancel(url: String) {
        innerCancel(url, false)
    }

    fun innerCancel(url: String, isNeedRestart: Boolean) {
        downloadTaskMap[url]?.let {
            if (it.mCurrentState == Status.NONE) {
                ThreadPool.threadPoolExecutor.remove(it)
                downloadCallbackMap[url]?.onCancel()
            } else {
                it.cancel(isNeedRestart)
            }
            downloadTaskMap.remove(url)
        }
    }

    fun restart(url: String) {
        if (downloadTaskMap.containsKey(url)) {
            downloadTaskMap[url]?.let {
                if (it.mCurrentState == Status.FINISH) {
                    downloadTaskMap.remove(url)
                    innerRestart(url)
                    return
                }
            }
        }

        if (!downloadTaskMap.containsKey(url)) {
            innerRestart(url)
        } else {
            innerCancel(url, true)
        }
    }

    fun innerRestart(url: String) {
        execute(downloadInfoMap[url]!!, downloadCallbackMap[url]!!)
    }

    fun destroy(url: String) {
        if (downloadTaskMap.containsKey(url)) {
            downloadTaskMap[url]?.destroy()
            downloadTaskMap.remove(url)
            downloadCallbackMap.remove(url)
            downloadInfoMap.remove(url)
        }
    }

    fun destroy(vararg urls: String) {
        urls.forEach { destroy(it) }
    }
}