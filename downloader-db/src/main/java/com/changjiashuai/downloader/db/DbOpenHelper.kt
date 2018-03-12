package com.changjiashuai.downloader.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/9 10:20.
 */
class DbOpenHelper(
    context: Context?,
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int
) : SQLiteOpenHelper(context, name, factory, version) {

    companion object {
        const val CREATE_DOWNLOAD_INFO_TABLE = """
           create table download_info(
           id integer primary key autoincrement,
           url text,
           path text,
           name text,
           child_task_count integer,
           current_length integer,
           total_length integer,
           percentage real,
           status integer,
           last_modify text,
           date text
           )
        """
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_DOWNLOAD_INFO_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("alter table download_info add column status integer")
    }
}