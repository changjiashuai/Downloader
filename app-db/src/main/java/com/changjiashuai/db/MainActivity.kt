package com.changjiashuai.db

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.changjiashuai.downloader.DownloadManager
import com.changjiashuai.downloader.callback.DownloadCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    val url = "http://1.198.5.23/imtt.dd.qq.com/16891/B8723A0DB2F2702C04D801D9FD19822C.apk"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("TAG", "onCreate")
        val path = "${Environment.getExternalStorageDirectory()}/downloaddb/"
        DownloadManager.getInstance(applicationContext)
            .init(url = url, path = path, name = "test.apk", childTaskCount = 3)

        button.setOnClickListener { start() }
        button2.setOnClickListener { pause() }
        button3.setOnClickListener { resume() }
        button4.setOnClickListener { cancel() }
    }

    private fun start() {
        DownloadManager.getInstance(applicationContext).start(object : DownloadCallback {
            override fun onStart(currentSize: Long, totalSize: Long, progress: Float) {
                Log.i(
                    "TAG",
                    "onStart currentSize=$currentSize, totalSize=$totalSize, progress=$progress"
                )
            }

            override fun onProgress(currentSize: Long, totalSize: Long, progress: Float) {
                Log.i(
                    "TAG",
                    "onProgress currentSize=$currentSize, totalSize=$totalSize, progress=$progress"
                )
            }

            override fun onPause() {
                Log.i("TAG", "onPause")
            }

            override fun onCancel() {
                Log.i("TAG", "onCancel")
            }

            override fun onFinish(file: File) {
                Log.i("TAG", "onFinish file=${file.absolutePath}")
            }

            override fun onWait() {
                Log.i("TAG", "onWait")
            }

            override fun onError(error: String) {
                Log.i("TAG", "onError")
            }
        })
    }

    private fun pause() {
        DownloadManager.getInstance(applicationContext).pause(url)
    }

    private fun resume() {
        DownloadManager.getInstance(applicationContext).resume(url)
    }

    private fun cancel() {
        DownloadManager.getInstance(applicationContext).cancel(url)
    }

    override fun onDestroy() {
        DownloadManager.getInstance(applicationContext).destroy(url)
        super.onDestroy()
    }
}
