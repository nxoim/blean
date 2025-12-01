package com.nxoim.blean.ui.screens.authentication

import BuildConfig
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.client.AccountManager
import com.nxoim.blean.commonThingsDumpster.childCoroutineScope
import com.nxoim.blean.shared.navigation.childStack
import com.nxoim.blean.shared.navigation.findInstance
import com.nxoim.blean.ui.RootNavigator
import com.nxoim.blean.ui.architecture.coroutineScope
import com.nxoim.blean.ui.screens.authentication.login.AuthorizationSource
import com.nxoim.blean.ui.screens.authentication.login.LoggedInUserBasicDetails
import com.nxoim.blean.ui.screens.authentication.login.LoginModel
import com.nxoim.blean.ui.screens.authentication.login.LoginNavigator
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

class AuthenticationScope(
    val accountManager: AccountManager,
    private val rootNavigator: RootNavigator,
    context: ComponentContext,
) {
    private val navigator = AuthenticationNavigatorImpl(rootNavigator)

    val stack = context.childStack(
        navigator,
        initialConfiguration = AuthenticationDestination.Login,
        childFactory = { destination, componentContext ->
            when (destination) {
                AuthenticationDestination.Login -> AuthenticationDestinationInstance.Login(
                    LoginModel(
                        defaultServerUrl = BuildConfig.Environment.defaultBskyServerEndpoint,
                        authorizationSource = LoginAuthorizationSource(accountManager),
                        loginNavigator = LoginNavigatorImpl(navigator, rootNavigator),
                        coroutineScope = componentContext.coroutineScope.childCoroutineScope()
                    )
                )
            }
        }
    )

    fun continueOauthAuthorization(rawUri: String) {
        navigator.navigateToLogin()

        val loginInstance = stack.findInstance<AuthenticationDestinationInstance.Login>()
            ?: error("Login instance not found when tried to continue oauth authorization")

        loginInstance.model.continueOauthAuthorization(rawUri)
    }
}


interface AuthenticationNavigator {
    fun navigateToUser(userDid: AccountIdentificator.Did)
    fun navigateToLogin()
}

class AuthenticationNavigatorImpl(
    private val rootNavigator: RootNavigator,
    private val navigator: StackNavigation<AuthenticationDestination> = StackNavigation()
) : AuthenticationNavigator, StackNavigation<AuthenticationDestination> by navigator {
    override fun navigateToUser(userDid: AccountIdentificator.Did) {
        rootNavigator.navigateToUser(userDid)
    }

    override fun navigateToLogin() {
        navigator.pushNew(AuthenticationDestination.Login)
    }
}

@Serializable
sealed interface AuthenticationDestination {
    @Serializable
    data object Login : AuthenticationDestination
}

sealed interface AuthenticationDestinationInstance {
    @JvmInline
    value class Login(val model: LoginModel) : AuthenticationDestinationInstance
}

private class LoginAuthorizationSource(
    private val accountManager: AccountManager
) : AuthorizationSource {
    override suspend fun beginOauthAuthorization(host: String): Result<String, Any> {
        return accountManager.beginOauthAuthorization(host)
    }

    override suspend fun continueOauthAuthorization(rawUri: String): Result<LoggedInUserBasicDetails, Any> {
        return accountManager.continueOauthAuthorization(rawUri)
            .map {
                LoggedInUserBasicDetails(
                    it.did,
                    it.handle,
                    it.displayName,
                    it.avatarUrl
                )
            }
    }
}

private class LoginNavigatorImpl(
    private val navigator: AuthenticationNavigator,
    private val rootNavigator: RootNavigator
) : LoginNavigator {
    override fun navigateToUser(did: AccountIdentificator.Did) = navigator.navigateToUser(did)

    override fun navigateToWebView(to: String) = rootNavigator.navigateToWebView(to)
}
