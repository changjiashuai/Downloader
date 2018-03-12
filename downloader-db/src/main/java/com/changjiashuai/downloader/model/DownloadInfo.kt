package com.changjiashuai.downloader.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/8 17:38.
 */
data class DownloadInfo(
    var url: String? = null,
    var path: String? = null,
    var name: String? = null,
    var currentLength: Long? = null,
    var totalLength: Long? = null,
    var percentage: Float? = null,
    var status: Int? = null,
    var childTaskCount: Int? = null,
    var date: Long? = null,
    var lastModify: String? = null
) : Parcelable {
    constructor(source: Parcel) : this(
        source.readString(),
        source.readString(),
        source.readString(),
        source.readValue(Long::class.java.classLoader) as Long?,
        source.readValue(Long::class.java.classLoader) as Long?,
        source.readValue(Float::class.java.classLoader) as Float?,
        source.readValue(Int::class.java.classLoader) as Int?,
        source.readValue(Int::class.java.classLoader) as Int?,
        source.readValue(Long::class.java.classLoader) as Long?,
        source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(url)
        writeString(path)
        writeString(name)
        writeValue(currentLength)
        writeValue(totalLength)
        writeValue(percentage)
        writeValue(status)
        writeValue(childTaskCount)
        writeValue(date)
        writeString(lastModify)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DownloadInfo> = object : Parcelable.Creator<DownloadInfo> {
            override fun createFromParcel(source: Parcel): DownloadInfo = DownloadInfo(source)
            override fun newArray(size: Int): Array<DownloadInfo?> = arrayOfNulls(size)
        }
    }
}