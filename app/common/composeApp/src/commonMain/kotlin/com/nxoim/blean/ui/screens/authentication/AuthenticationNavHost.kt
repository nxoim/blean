package com.nxoim.blean.ui.screens.authentication

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.nxoim.blean.ui.screens.authentication.login.LoginScreen

@Composable
fun AuthenticationNavHost(
    model: AuthenticationScope,
) {
    Children(model.stack) {
        val instance = it.instance

        when (instance) {
            is AuthenticationDestinationInstance.Login -> LoginScreen(instance.model)
        }
    }
}

