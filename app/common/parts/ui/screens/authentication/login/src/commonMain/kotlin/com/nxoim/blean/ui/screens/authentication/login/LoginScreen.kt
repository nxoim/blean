package com.nxoim.blean.ui.screens.authentication.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.nxoim.blean.ui.composeMaterial3Extensions.BasicFullscreenLoadingBar
import com.nxoim.blean.ui.screens.authentication.login.components.WelcomeSignInScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(model: LoginModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedContent(model.loginState) { loginState ->
            when (loginState) {
                is WebViewLinkThingState.Idle -> WelcomeSignInScreen(model)

                WebViewLinkThingState.Loading -> BasicFullscreenLoadingBar()

                WebViewLinkThingState.ErrorCreatingTheLink -> {
                    Text("Error")
                }

                is WebViewLinkThingState.ReadyToLogin -> Column {
                    // several reasons to keep web view:
                    // 1. i aint paying apples developer license anytime soon
                    // 2. in order not to leave the user hanging, if some strange
                    // error occurs, the app needs to make sure it can open
                    // the callback link by checking permissions to open links.
                    // The availability of such system functionality is not guaranteed
                    // to be on any platform thats not android.
                    // 3. will be polluting people's tab history

                    // reasons to keep pushing for app links and shit:
                    // 1. potential privacy concerns
                    // 2. some maintenance (app links and whatever
                    // arguably require too much maintenance anyway)
                    // 3. user will have to insert their credentials
                    // again if they were logged in via browser
                    val webviewState = rememberWebViewState(
                        url = loginState.url,
                    )
                    val webviewNavigator = rememberWebViewNavigator()

                    TopAppBar(
                        title = { Text(webviewState.lastLoadedUrl?.removePath() ?: "") },
                        navigationIcon = {
                            IconButton(onClick = { model.reset() }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        },
                        actions = {
                            val coroutineScope = rememberCoroutineScope()
                            var isDropDownVisible by rememberSaveable { mutableStateOf(false) }

                            IconButton(
                                onClick = { isDropDownVisible = !isDropDownVisible }
                            ) {
                                Icon(Icons.Default.MoreVert, null)

                                DropdownMenu(
                                    expanded = isDropDownVisible,
                                    onDismissRequest = { isDropDownVisible = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Reload") },
                                        onClick = {
                                            isDropDownVisible = false
                                            webviewNavigator.reload()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete all cookies") },
                                        onClick = {
                                            isDropDownVisible = false
                                            coroutineScope.launch {
                                                webviewState.cookieManager.removeAllCookies()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )

                    WebView(
                        state = webviewState,
                        navigator = webviewNavigator,
                        modifier = Modifier.fillMaxSize()
                    )

                    LaunchedEffect(webviewState.lastLoadedUrl) {
                        model.processLastLoadedUrlFromWebView(webviewState.lastLoadedUrl)
                    }
                }

                WebViewLinkThingState.LoggingIn -> BasicFullscreenLoadingBar()

                WebViewLinkThingState.RequestRejected -> Column {
                    Text("Access was rejected")
                    Button(onClick = { model.attemptLogin() }) {
                        Text("Retry")
                    }
                }

                is WebViewLinkThingState.ErrorLoggingIn -> Column {
                    Text(loginState.message)
                    Button(onClick = { model.attemptLogin() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

private fun String.removePath(): String {
    // Find the start of the path by looking for the first single slash (/) after the host
    val schemeSeparatorIndex = this.indexOf("://")
    if (schemeSeparatorIndex == -1) return this // If there's no scheme, return as is

    val hostStartIndex = schemeSeparatorIndex + 3
    val pathStartIndex = this.indexOf("/", hostStartIndex)

    return if (pathStartIndex == -1) {
        // If there's no path, return the whole URL
        this
    } else {
        // Keep everything up to the start of the path
        this.substring(0, pathStartIndex)
    }
}


