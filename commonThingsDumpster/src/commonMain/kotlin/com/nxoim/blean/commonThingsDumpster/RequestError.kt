package com.nxoim.blean.commonThingsDumpster

import kotlin.jvm.JvmInline

sealed interface RequestError<out OtherErrorType> {
    data class Http<OtherErrorType>(
        val code: Int,
        val description: String,
        val responseBody: String?
    ) : RequestError<OtherErrorType>

    object Timeout : RequestError<Nothing>  {
        operator fun invoke() = this
    }

    object CantConnectToInternet : RequestError<Nothing> {
        operator fun invoke() = this
    }

    @JvmInline
    value class Other<T>(val value: T) : RequestError<T>

    object UnresolvedAddress : RequestError<Nothing>  {
        operator fun invoke() = this
    }

    data class Internal<OtherErrorType>(
        val exception: Throwable
    ) : RequestError<OtherErrorType>
}