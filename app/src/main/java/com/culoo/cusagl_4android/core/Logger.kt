package com.culoo.cusagl_4android.core

interface Logger {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

object LogTags {
    const val PARSE_FAIL = "PARSE_FAIL"
    const val FILE_MISSING = "FILE_MISSING"
    const val CACHE_INVALID = "CACHE_INVALID"
}

object DefaultLogger : Logger {
    override fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        println("W/$tag: $message")
        throwable?.printStackTrace()
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println("E/$tag: $message")
        throwable?.printStackTrace()
    }
}

