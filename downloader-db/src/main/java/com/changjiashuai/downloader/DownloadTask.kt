package com.changjiashuai.downloader

import android.content.Context
import android.util.Log
import com.changjiashuai.downloader.callback.DownloadCallback
import com.changjiashuai.downloader.db.DbManager
import com.changjiashuai.downloader.ext.isNotServerFileChanged
import com.changjiashuai.downloader.ext.isSupportRange
import com.changjiashuai.downloader.ext.lastModify
import com.changjiashuai.downloader.model.DownloadInfo
import com.changjiashuai.downloader.model.Range
import com.changjiashuai.downloader.model.Status
import com.changjiashuai.downloader.net.OkHttpManager
import com.changjiashuai.downloader.utils.FileUtils
import com.changjiashuai.downloader.utils.Utils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/9 11:40.
 */
class DownloadTask(private val context: Context, downloadInfo: DownloadInfo) : Runnable {

    private val url: String = downloadInfo.url!!
    private val path: String = downloadInfo.path!!
    private val name: String = downloadInfo.name!!
    private var childTaskCount = downloadInfo.childTaskCount!!
    private var tempFileTotalSize: Long = EACH_TEMP_SIZE * childTaskCount.toLong()
    private var tempChildTaskCount = 0
    private val callList: ArrayList<Call> by lazy { arrayListOf<Call>() }

    var mCurrentState = Status.NONE

    var downloadCallback: DownloadCallback? = null

    //记录已经下载的大小
    private var currentLength = 0L
    //记录文件总大小
    private var totalLength = 0L
    //刷新频率控制
    private var lastProgressTime: Long = 0
    //是否支持断点续传
    private var isSupportRange: Boolean = false
    //重新开始下载需要先进行取消操作
    private var isNeedRestart: Boolean = false

    companion object {
        const val EACH_TEMP_SIZE = 16
        const val BUFFER_SIZE = 4096

        var IS_PAUSE = false
        var IS_CANCEL = false
        var IS_DESTROY = false
    }

    override fun run() {
        try {
            val saveFile = File(path, name)
            val tempFile = File(path, "$name.temp")
            val downloadInfo = DbManager.getInstance(context).query(url)
            if (saveFile.exists() && tempFile.exists()
                && downloadInfo != null && downloadInfo.status != Status.DOWNLOADING) {
                //断点下载
                val response = OkHttpManager.initRequest(url, downloadInfo.lastModify!!)
                if (response.isSuccessful && response.isNotServerFileChanged()) {
                    tempFileTotalSize = EACH_TEMP_SIZE * downloadInfo.childTaskCount!!.toLong()
                    isSupportRange = true
                    currentLength = downloadInfo.currentLength!!
                    totalLength = downloadInfo.totalLength!!
                    onStart(
                        currentLength,
                        totalLength,
                        "",
                        isSupportRange
                    )
                } else {
                    prepareRangeFile(response)
                }
                saveRangeFile()
            } else {
                //新的下载
                val response = OkHttpManager.initRequest(url)
                if (response.isSuccessful) {
                    if (response.isSupportRange()) {
                        prepareRangeFile(response)
                        saveRangeFile()
                    } else {
                        saveCommonFile(response)
                    }
                }
            }
        } catch (e: IOException) {

        }
    }

    private fun prepareRangeFile(response: Response) {
        try {
            var saveFile: File? = File(path, name)
            var tempFile: File? = File(path, "$name.temp")
            response.use {
                val contentLength = response.body()?.contentLength()
                isSupportRange = true
                totalLength = contentLength!!
                onStart(0, totalLength, response.lastModify()!!, isSupportRange)

                DbManager.getInstance(context).delete(url)
                FileUtils.deleteFile(saveFile, tempFile)

                saveFile = FileUtils.createFile(path, name)
                tempFile = FileUtils.createFile(path, "$name.temp")

                val saveRandomAccessFile = RandomAccessFile(saveFile, "rws")
                saveRandomAccessFile.use { it.setLength(contentLength) }
                val tempRandomAccessFile = RandomAccessFile(tempFile, "rws")
                tempRandomAccessFile.use {
                    it.setLength(tempFileTotalSize)
                    val tempChannel = it.channel
                    tempChannel.use {
                        val buffer = it.map(FileChannel.MapMode.READ_WRITE, 0, tempFileTotalSize)
                        var start: Long
                        var end: Long
                        val eachSize = contentLength / childTaskCount
                        for (i in 0 until childTaskCount) {
                            if (i == childTaskCount - 1) {
                                start = i * eachSize
                                end = contentLength - 1
                            } else {
                                start = i * eachSize
                                end = (i + 1) * eachSize - 1
                            }
                            buffer.putLong(start)
                            buffer.putLong(end)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onError(e.toString())
        }
    }

    private fun saveRangeFile() {
        val saveFile = FileUtils.createFile(path, name)
        val tempFile = FileUtils.createFile(path, "$name.temp")
        val range = readDownloadRange(tempFile)

        DbManager.getInstance(context).update(0, 0f, Status.DOWNLOADING, url)
        for (i in 0 until childTaskCount) {
            val call = OkHttpManager.initRequest(url, range.start[i], range.end[i],
                object : Callback {
                    override fun onFailure(call: Call?, e: IOException) {
                        onError(e.toString())
                    }

                    override fun onResponse(call: Call?, response: Response) {
                        startSaveRangeFile(response, i, range, saveFile!!, tempFile!!)
                    }
                })
            callList.add(call)
        }
        while (tempChildTaskCount < childTaskCount) {
            //TODO ???
            //由于每个文件采用多个异步操作进行，发起多个异步操作后该线程已经结束，但对应文件并未下载完成，
            //则会出现线程池中同时下载的文件数量超过设定的核心线程数，所以考虑只有当前线程的所有异步任务结束后，
            //才能使结束当前线程。
        }
    }

    @Synchronized
    private fun addCount() {
        tempChildTaskCount++
    }

    private fun readDownloadRange(tempFile: File?): Range {
        val record = RandomAccessFile(tempFile, "rws")
        val channel = record.channel
        val buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, tempFileTotalSize)
        val startBytes = LongArray(childTaskCount)
        val endBytes = LongArray(childTaskCount)
        for (i in 0 until childTaskCount) {
            startBytes[i] = buffer.long
            endBytes[i] = buffer.long
        }
        return Range(startBytes, endBytes)
    }

    private fun startSaveRangeFile(
        response: Response,
        index: Int,
        range: Range,
        saveFile: File,
        tempFile: File
    ) {
        val saveRandomAccessFile = RandomAccessFile(saveFile, "rws")
        val saveChannel = saveRandomAccessFile.channel
        val saveBuffer = saveChannel.map(
            FileChannel.MapMode.READ_WRITE,
            range.start[index],
            range.end[index] - range.start[index] + 1
        )
        val tempRandomAccessFile = RandomAccessFile(tempFile, "rws")
        val tempChannel = tempRandomAccessFile.channel
        val tempBuffer = tempChannel.map(FileChannel.MapMode.READ_WRITE, 0, tempFileTotalSize)
        val inputStream = response.body()?.byteStream()

        var len = 0
        val buffer = ByteArray(BUFFER_SIZE)

        while (inputStream?.read(buffer).apply { len = this ?: -1 } != -1) {
            //if cancel break
            if (IS_CANCEL) {
                onCancel()
                callList[index].cancel()
                break
            }

            saveBuffer.put(buffer, 0, len)
            tempBuffer.putLong(
                index * EACH_TEMP_SIZE,
                tempBuffer.getLong(index * EACH_TEMP_SIZE) + len
            )
            onProgress(len.toLong())

            //if destroy break
            if (IS_DESTROY) {
                onDestroy()
                callList[index].cancel()
                break
            }

            //if pause break
            if (IS_PAUSE) {
                onPause()
                callList[index].cancel()
                break
            }
        }
        addCount()
    }

    private fun saveCommonFile(response: Response) {
        val contentLength = response.body()?.contentLength()
        isSupportRange = false
        totalLength = contentLength ?: -1
        onStart(0, totalLength, "", isSupportRange)
        FileUtils.deleteFile(path, name)
        val file = FileUtils.createFile(path, name) ?: return

        val inputStream = response.body()?.byteStream()
        val outputStream = FileOutputStream(file)

        var len = 0
        val buffer = ByteArray(BUFFER_SIZE)

        while (inputStream?.read(buffer).apply { len = this ?: -1 } != -1) {
            if (IS_CANCEL) {
                onCancel()
                break
            }
            if (IS_DESTROY) {
                break
            }
            outputStream.write(buffer, 0, len)
            onProgress(len.toLong())
        }
        outputStream.flush()
    }

    private fun onStart(
        currentLength: Long,
        totalLength: Long,
        lastModify: String,
        isSupportRange: Boolean
    ) {
        mCurrentState = Status.START
        if (!isSupportRange) {
            childTaskCount = 1
        } else if (currentLength == 0L) {
            DbManager.getInstance(context).insert(
                DownloadInfo(
                    url = url,
                    path = path,
                    childTaskCount = childTaskCount,
                    name = name,
                    currentLength = currentLength,
                    totalLength = totalLength,
                    lastModify = lastModify,
                    date = System.currentTimeMillis()
                )
            )
        }
        downloadCallback?.onStart(
            currentLength,
            totalLength,
            Utils.getPercentage(currentLength, totalLength)
        )
    }

    @Synchronized
    private fun onProgress(length: Long) {
        mCurrentState = Status.DOWNLOADING
        currentLength += length

        //TODO ???
        if (System.currentTimeMillis() - lastProgressTime >= 20 || currentLength <= totalLength) {
            downloadCallback?.onProgress(
                currentLength,
                totalLength,
                Utils.getPercentage(currentLength, totalLength)
            )
            lastProgressTime = System.currentTimeMillis()
        }
        onFinish()
    }

    private fun onFinish() {
        if (currentLength == totalLength) {
            if (isSupportRange) {
                FileUtils.deleteFile(File(path, "$name.temp"))
                DbManager.getInstance(context).delete(url)
            }
            downloadCallback?.onFinish(File(path, name))
        }
    }

    private fun onError(error: String) {
        mCurrentState = Status.ERROR
        if (isSupportRange) {
            DbManager.getInstance(context).update(
                currentLength,
                Utils.getPercentage(currentLength, totalLength),
                Status.ERROR,
                url
            )
        }
        downloadCallback?.onError(error)
    }

    @Synchronized
    private fun onPause() {
        if (isSupportRange) {
            DbManager.getInstance(context).update(
                currentLength,
                Utils.getPercentage(currentLength, totalLength),
                Status.PAUSE,
                url
            )
        }
        tempChildTaskCount++
        if (tempChildTaskCount == childTaskCount) {
            downloadCallback?.onPause()
            tempChildTaskCount = 0
        }
    }

    @Synchronized
    private fun onCancel() {
        val lastState = mCurrentState
        tempChildTaskCount++
        if (tempChildTaskCount == childTaskCount || lastState == Status.PAUSE || lastState == Status.ERROR) {
            tempChildTaskCount = 0
            downloadCallback?.onProgress(0, totalLength, 0f)
            currentLength = 0
            if (isSupportRange) {
                DbManager.getInstance(context).delete(url)
                FileUtils.deleteFile(File(path, "$name.temp"))
            }
            FileUtils.deleteFile(File(path, name))
            downloadCallback?.onCancel()
            if (isNeedRestart) {
                isNeedRestart = false
                //TODO restart download ???
                DownloadManager.getInstance(context).innerRestart(url)
            }
        }
    }

    @Synchronized
    private fun onDestroy() {
        if (isSupportRange) {
            DbManager.getInstance(context).update(
                currentLength,
                Utils.getPercentage(currentLength, totalLength),
                Status.DESTROY,
                url
            )
        }
    }

    /**
     * 暂停（正在下载才可以暂停）
     * 如果文件不支持断点续传则不能进行暂停操作
     */
    fun pause() {
        if (mCurrentState == Status.DOWNLOADING) {
            IS_PAUSE = true
            mCurrentState = Status.PAUSE
            Log.i("DownloadTask", "pause() status = $mCurrentState")
        }
    }

    /**
     * 取消（已经被取消、下载结束则不可取消）
     */
    fun cancel(isNeedRestart: Boolean) {
        this.isNeedRestart = isNeedRestart
        if (mCurrentState == Status.DOWNLOADING) {
            IS_CANCEL = true
            mCurrentState = Status.CANCEL
        } else if (mCurrentState == Status.PAUSE || mCurrentState == Status.ERROR) {
            onCancel()
        }
    }

    /**
     * 下载中退出时保存数据、释放资源
     */
    fun destroy() {
        if (mCurrentState == Status.CANCEL || mCurrentState == Status.PAUSE) {
            return
        }
        IS_DESTROY = true
        mCurrentState = Status.DESTROY
    }
}