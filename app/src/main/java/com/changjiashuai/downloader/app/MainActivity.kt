package com.changjiashuai.downloader.app

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.changjiashuai.downloader.DownloadInfo
import com.changjiashuai.downloader.DownloadManager
import com.changjiashuai.downloader.DownloadObserver
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val url1 = "http://1.198.5.23/imtt.dd.qq.com/16891/B8723A0DB2F2702C04D801D9FD19822C.apk"
        const val url2 = ""
        const val url3 = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DownloadManager.mDownloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

        main_btn_down1.setOnClickListener { testDownload1() }
        main_btn_cancel1.setOnClickListener { testCancel1() }
    }

    private fun testDownload1() {
        Log.i("TAG", "testDownload1")
        DownloadManager.download(url1, object : DownloadObserver() {

            override fun onNext(downloadInfo: DownloadInfo) {
                super.onNext(downloadInfo)
                Log.i("TAG", "progress=${downloadInfo.progress}")
                main_progress1.max = downloadInfo.totalSize.toInt()
                main_progress1.progress = downloadInfo.progress.toInt()
            }

            override fun onComplete() {
                Toast.makeText(
                    this@MainActivity,
                    downloadInfo.fileName + "-DownloadComplete",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun testCancel1() {
        DownloadManager.cancel(url1)
    }
}
