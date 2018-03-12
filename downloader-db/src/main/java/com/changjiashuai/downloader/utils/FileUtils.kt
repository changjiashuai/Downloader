package com.changjiashuai.downloader.utils

import android.text.TextUtils
import java.io.File
import java.io.IOException

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/9 17:01.
 */
internal class FileUtils {

    companion object {

        fun deleteFile(vararg files: File?) {
            for (file in files) {
                deleteFile(file)
            }
        }

        fun deleteFile(file: File?): Boolean {
            return if (file != null && file.exists()) {
                file.delete()
            } else false
        }

        fun deleteFile(path: String, name: String) {
            deleteFile(File(path, name))
        }

        @Synchronized
        fun createFile(path: String, name: String): File? {
            if (TextUtils.isEmpty(path) || TextUtils.isEmpty(name)) {
                return null
            }

            val parentFile = File(path)
            if (!parentFile.exists()) {
                parentFile.mkdir()
            }

            val file = File(parentFile, name)
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            return file
        }

    }
}