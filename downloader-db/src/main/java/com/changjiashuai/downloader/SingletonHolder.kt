package com.changjiashuai.downloader

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/12 14:29.
 */
open class SingletonHolder<out T, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T {
        var i = instance
        if (i !== null) return i
        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}