package com.changjiashuai.downloader.utils

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/12 10:42.
 */
class Utils {

    companion object {

        fun getPercentage(currentSize: Long, totalSize: Long): Float {
            return if (currentSize > totalSize) {
                0f
            } else (currentSize * 10000.0 / totalSize).toInt() * 1.0f / 100

        }
    }
}