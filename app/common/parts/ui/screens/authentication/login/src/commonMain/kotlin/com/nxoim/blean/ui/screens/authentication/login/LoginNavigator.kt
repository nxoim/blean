package com.nxoim.blean.ui.screens.authentication.login

import com.nxoim.blean.bskyPrimitives.AccountIdentificator

interface LoginNavigator {
    fun navigateToUser(did: AccountIdentificator.Did)
    fun navigateToWebView(to: String)
}
