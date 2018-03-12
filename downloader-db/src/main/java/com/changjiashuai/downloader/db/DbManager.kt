package com.changjiashuai.downloader.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.changjiashuai.downloader.model.DownloadInfo
import com.changjiashuai.downloader.model.Status

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/9 10:30.
 */
class DbManager {

    private var sqlDb: SQLiteDatabase

    private constructor(context: Context) {
        val dbHelper = DbOpenHelper(context.applicationContext, DB_NAME, null, VERSION)
        sqlDb = dbHelper.writableDatabase
    }


    companion object {
        private const val DB_NAME = "db_download"
        private const val VERSION = 1
        private const val TABLE_DOWNLOAD = "download_info"

        @Volatile
        private var instance: DbManager? = null

        fun getInstance(context: Context): DbManager {
            if (instance == null) {
                synchronized(DbManager::class) {
                    if (instance == null) {
                        instance = DbManager(context.applicationContext)
                    }
                }
            }
            return instance!!
        }
    }

    fun insert(downloadInfo: DownloadInfo) {
        val values = ContentValues()
        values.put("url", downloadInfo.url)
        values.put("path", downloadInfo.path)
        values.put("name", downloadInfo.name)
        values.put("child_task_count", downloadInfo.childTaskCount)
        values.put("current_length", downloadInfo.currentLength)
        values.put("total_length", downloadInfo.totalLength)
        values.put("percentage", downloadInfo.percentage)
        values.put("status", downloadInfo.status)
        values.put("last_modify", downloadInfo.lastModify)
        values.put("date", downloadInfo.date)
        sqlDb.insert(TABLE_DOWNLOAD, null, values)
    }

    fun insertAll(downloadInfos: List<DownloadInfo>) {
        downloadInfos.forEach { insert(it) }
    }

    fun query(url: String): DownloadInfo? {
        val cursor = sqlDb.query(TABLE_DOWNLOAD, null, "url = ?", arrayOf(url), null, null, null)
        if (!cursor.moveToFirst()) {
            return null
        }

        val downloadInfo = DownloadInfo()
        downloadInfo.url = cursor.getString(cursor.getColumnIndex("url"))
        downloadInfo.path = cursor.getString(cursor.getColumnIndex("path"))
        downloadInfo.name = cursor.getString(cursor.getColumnIndex("name"))
        downloadInfo.childTaskCount = cursor.getInt(cursor.getColumnIndex("child_task_count"))
        downloadInfo.currentLength = cursor.getLong(cursor.getColumnIndex("current_length"))
        downloadInfo.totalLength = cursor.getLong(cursor.getColumnIndex("total_length"))
        downloadInfo.percentage = cursor.getFloat(cursor.getColumnIndex("percentage"))
        downloadInfo.status = cursor.getInt(cursor.getColumnIndex("status"))
        downloadInfo.lastModify = cursor.getString(cursor.getColumnIndex("last_modify"))
        downloadInfo.date = cursor.getLong(cursor.getColumnIndex("date"))
        cursor.close()
        return downloadInfo
    }

    fun queryAll(): List<DownloadInfo> {
        val list = arrayListOf<DownloadInfo>()
        val cursor = sqlDb.query(TABLE_DOWNLOAD, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val downloadInfo = DownloadInfo()
                downloadInfo.url = cursor.getString(cursor.getColumnIndex("url"))
                downloadInfo.path = cursor.getString(cursor.getColumnIndex("path"))
                downloadInfo.name = cursor.getString(cursor.getColumnIndex("name"))
                downloadInfo.childTaskCount =
                        cursor.getInt(cursor.getColumnIndex("child_task_count"))
                downloadInfo.currentLength = cursor.getLong(cursor.getColumnIndex("current_length"))
                downloadInfo.totalLength = cursor.getLong(cursor.getColumnIndex("total_length"))
                downloadInfo.percentage = cursor.getFloat(cursor.getColumnIndex("percentage"))
                downloadInfo.status = cursor.getInt(cursor.getColumnIndex("status"))
                downloadInfo.lastModify = cursor.getString(cursor.getColumnIndex("last_modify"))
                downloadInfo.date = cursor.getLong(cursor.getColumnIndex("date"))
                list.add(downloadInfo)
            } while (cursor.moveToNext())
        }
        return list
    }

    fun update(currentSize: Long, percentage: Float, status: Int, url: String) {
        val values = ContentValues()
        if (status != Status.DOWNLOADING) {
            values.put("current_length", currentSize)
            values.put("percentage", percentage)
        }
        values.put("status", status)
        sqlDb.update(TABLE_DOWNLOAD, values, "url = ?", arrayOf(url))
    }

    fun delete(url: String) {
        sqlDb.delete(TABLE_DOWNLOAD, "url = ?", arrayOf(url))
    }
}