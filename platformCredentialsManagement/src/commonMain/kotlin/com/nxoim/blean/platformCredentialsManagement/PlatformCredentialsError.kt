package com.nxoim.blean.platformCredentialsManagement

sealed interface PlatformCredentialsRetrievalError {
    data class DecodingError(val message: String) : PlatformCredentialsRetrievalError
    data class UnknownError(val message: String) : PlatformCredentialsRetrievalError
}

sealed interface PlatformCredentialsSavingError {
    data class EncodingError(val message: String) : PlatformCredentialsSavingError
    data class UnknownError(val message: String) : PlatformCredentialsSavingError
}

sealed interface PlatformCredentialsRemovalError {
    data class UnknownError(val message: String) : PlatformCredentialsRemovalError
}