package com.nxoim.blean.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.nxoim.blean.ui.screens.authentication.AuthenticationNavHost
import com.nxoim.blean.ui.screens.content.ContentRootScreen
import com.nxoim.blean.ui.theme.AppTheme

@Composable
fun RootNavHost(
    scope: RootScope,
) {
    AppTheme {
        Surface(Modifier.fillMaxSize()) {
            AnimatedContent(scope.state.collectAsState().value) { state ->
                when (state) {
                    is RootScopeState.Initialized -> {
                        NavigatorOfInitializedContent(state)
                    }

                    RootScopeState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Pretend this is a splash screen")
                        }
                    }

                    is RootScopeState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.readableMessage)
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun NavigatorOfInitializedContent(state: RootScopeState.Initialized) {
    Children(state.scope.stack) { child ->
        when (val instance = child.instance) {
            is RootDestinationInstance.Authentication -> {
                AuthenticationNavHost(instance.scope)
            }

            is RootDestinationInstance.User -> {
                ContentRootScreen(instance.scope)
            }
        }
    }
}

