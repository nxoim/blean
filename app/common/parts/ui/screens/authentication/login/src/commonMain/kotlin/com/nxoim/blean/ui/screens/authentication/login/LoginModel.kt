package com.nxoim.blean.ui.screens.authentication.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val logTag = "LoginModel"

class LoginModel(
    private val defaultServerUrl: String,
    private val authorizationSource: AuthorizationSource,
    private val loginNavigator: LoginNavigator,
    private val logger: Logger = Logger,
    private val coroutineScope: CoroutineScope
) {
    var deeplinkSimulation by mutableStateOf("")
    var serverUrl by mutableStateOf(defaultServerUrl)
    var loginState by mutableStateOf<WebViewLinkThingState>(WebViewLinkThingState.Idle)
        private set

    fun attemptLogin() {
        coroutineScope.launch {
            loginState = WebViewLinkThingState.Loading

            authorizationSource.beginOauthAuthorization(serverUrl)
                .onSuccess {
                    loginState = WebViewLinkThingState.ReadyToLogin(it)
                }
                .onFailure { loginState = WebViewLinkThingState.ErrorCreatingTheLink }
        }
    }

    fun performDeepLinkSim() {
        continueOauthAuthorization(deeplinkSimulation)

    }

    fun continueOauthAuthorization(rawUri: String) {
        loginState = WebViewLinkThingState.LoggingIn

        coroutineScope.launch {
            authorizationSource.continueOauthAuthorization(rawUri)
                .onSuccess {
                    logger.v(tag = logTag) { "Trying to navigate to user" }
                    withContext(Dispatchers.Main.immediate) {
                        loginNavigator.navigateToUser(it.did)
                    }

                    delay(1000)
                    loginState = WebViewLinkThingState.Idle

                    // and something else
                }
                .onFailure {
                    loginState = WebViewLinkThingState.ErrorLoggingIn(it.toString())
                    logger.e(tag = logTag) { "Failed continuing oauth authorization. $it" }
                }
        }
    }

    fun reset() {
        loginState = WebViewLinkThingState.Idle
    }

    fun processLastLoadedUrlFromWebView(lastLoadedUrl: String?) {
        logger.v(tag = logTag) { "Processing last loaded URL: $lastLoadedUrl" }

        if (lastLoadedUrl?.contains("error=") == true)
            reset()
        else if (lastLoadedUrl?.contains("oauth/authorize/reject") == true)
            loginState = WebViewLinkThingState.RequestRejected
        else if (lastLoadedUrl?.contains("/oauth2/callback") == true)
            continueOauthAuthorization(lastLoadedUrl)
        else if (lastLoadedUrl?.contains("error=") == true) {
            loginState =
                WebViewLinkThingState.ErrorLoggingIn(lastLoadedUrl.extractErrorType() + " " + lastLoadedUrl.extractErrorDescription())
        }
    }
}
private fun String.extractErrorType(): String = substringAfter("error=","")
    .substringBefore("&")

private fun String.extractErrorDescription(): String = substringAfter("error_description=", "")
    .substringBefore("&")
    .replace("+", " ")

sealed interface WebViewLinkThingState {
    data object Loading : WebViewLinkThingState
    data object Idle : WebViewLinkThingState
    data object RequestRejected : WebViewLinkThingState
    data object ErrorCreatingTheLink : WebViewLinkThingState
    data class ReadyToLogin(val url: String) : WebViewLinkThingState
    data object LoggingIn : WebViewLinkThingState
    data class ErrorLoggingIn(val message: String) : WebViewLinkThingState
}

interface AuthorizationSource {
    suspend fun beginOauthAuthorization(host: String): Result<String, Any>
    suspend fun continueOauthAuthorization(rawUri: String): Result<LoggedInUserBasicDetails, Any>
}

data class LoggedInUserBasicDetails(
    val did: AccountIdentificator.Did,
    val handle: AccountIdentificator.Handle,
    val displayName: String?,
    val avatarUrl: String?
)