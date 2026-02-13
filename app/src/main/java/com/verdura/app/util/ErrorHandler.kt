package com.verdura.app.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {

    fun getReadableErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "Connection timed out. Please try again."
            is IOException -> "Network error. Please check your connection and try again."
            else -> throwable.message ?: "An unexpected error occurred. Please try again."
        }
    }

    fun isNetworkError(throwable: Throwable): Boolean {
        return throwable is UnknownHostException ||
               throwable is SocketTimeoutException ||
               throwable is IOException
    }
}
