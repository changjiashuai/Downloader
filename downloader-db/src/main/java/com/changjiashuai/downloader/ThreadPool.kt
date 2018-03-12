package com.changjiashuai.downloader

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/12 11:29.
 */
object ThreadPool {
    //CPU核心数
    private val cpu_count = Runtime.getRuntime().availableProcessors()
    //可同时下载的任务数（核心线程数）
    var corePoolSize = 3
        set(value) {
            if (value == 0) return else field = value
        }
    //缓存队列的大小（最大线程数）
    var maxPoolSize = 20
        set(value) {
            if (value == 0) return else field = value
        }
    //非核心线程闲置的超时时间（秒），如果超时则会被回收
    private val KEEP_ALIVE = 10L

    private val threadFactory = object : ThreadFactory {

        private val count = AtomicInteger()

        override fun newThread(r: Runnable?): Thread {
            return Thread(r, "download_task#${count.getAndIncrement()}")
        }
    }

    val threadPoolExecutor: ThreadPoolExecutor by lazy {
        return@lazy ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            KEEP_ALIVE,
            TimeUnit.SECONDS,
            LinkedBlockingDeque<Runnable>(),
            threadFactory
        )
    }
}