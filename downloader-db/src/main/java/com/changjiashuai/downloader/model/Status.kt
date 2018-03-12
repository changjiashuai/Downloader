package com.changjiashuai.downloader.model

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2018/3/9 11:23.
 */
class Status {

    companion object {
        const val NONE = 0x1000 //无状态
        const val START = 0x1001 //准备下载
        const val DOWNLOADING = 0x1002 //下载中
        const val PAUSE = 0x1003 //暂停
        const val RESUME = 0x1004 //继续下载
        const val CANCEL = 0x1005 //取消
        const val RESTART = 0x1006 //重新下载
        const val FINISH = 0x1007 //下载完成
        const val ERROR = 0x1008 //下载出错
        const val WAIT = 0x1009 //等待中
        const val DESTROY = 0x1010 //释放资源
    }
}