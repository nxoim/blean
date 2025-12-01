package com.nxoim.blean.ui

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnResume
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.byteCount.megabytes
import com.nxoim.blean.client.Account
import com.nxoim.blean.client.AccountManager
import com.nxoim.blean.client.AccountManagerHolder
import com.nxoim.blean.client.AccountManagerState
import com.nxoim.blean.client.AccountsState
import com.nxoim.blean.composeVideoPlayer.VideoPlayerCacheConfiguration
import com.nxoim.blean.shared.Deeplink
import com.nxoim.blean.shared.DeeplinkStateFlow
import com.nxoim.blean.shared.InstanceCreationReason
import com.nxoim.blean.shared.PlatformInstanceManagement
import com.nxoim.blean.shared.appEnvironment.PathProvider
import com.nxoim.blean.shared.appEnvironment.StoragePaths
import com.nxoim.blean.shared.navigation.childStack
import com.nxoim.blean.shared.navigation.findInstance
import com.nxoim.blean.ui.architecture.coroutineScope
import com.nxoim.blean.ui.screens.authentication.AuthenticationScope
import com.nxoim.blean.ui.screens.content.ContentRootScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

private const val logTag = "RootScope"

class RootScope(
    val accountManagerHolder: AccountManagerHolder,
    val deeplinkFlow: DeeplinkStateFlow,
    private val instanceCreationReason: InstanceCreationReason,
    private val context: ComponentContext,
    private val onOpenWebView: (url: String) -> Unit,
    private val logger: Logger,
    private val instanceManagement: PlatformInstanceManagement
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val state = accountManagerHolder.state
        .flatMapLatest { mapManagerState(it) }
        .flowOn(Dispatchers.Main.immediate)
        .stateIn(
            context.coroutineScope,
            SharingStarted.WhileSubscribed(),
            RootScopeState.Loading
        )

    init {
        context.doOnResume {
            context.coroutineScope.launch(Dispatchers.Default) {
                accountManagerHolder.initializeIfNotInitialized()
            }
        }
    }

    private fun mapManagerState(state: AccountManagerState): Flow<RootScopeState> =
        when (state) {
            is AccountManagerState.Error ->
                flowOf(RootScopeState.Error(state.cause.stackTraceToString()))

            AccountManagerState.Idle,
            AccountManagerState.Initializing ->
                flowOf(RootScopeState.Loading)

            is AccountManagerState.Initialized ->
                mapAccountsState(state)
        }

    private fun mapAccountsState(
        state: AccountManagerState.Initialized
    ): Flow<RootScopeState> =
        state.manager.accountsState.map { accountsState ->
            when (accountsState) {
                is AccountsState.Loading -> RootScopeState.Loading

                is AccountsState.InitializationError ->
                    RootScopeState.Error(accountsState.toString())

                is AccountsState.Initialized ->
                    RootScopeState.Initialized(
                        InitializedRootScope(
                            accounts = accountsState.accounts,
                            accountManager = state.manager,
                            deeplinkFlow = deeplinkFlow,
                            onOpenWebView = onOpenWebView,
                            rootContext = context,
                            logger = logger
                        )
                    )
            }
        }
}

@Serializable
sealed interface RootDestination {
    @Serializable
    data object Authentication : RootDestination

    @Serializable
    data class User(val userDid: AccountIdentificator.Did) : RootDestination
}


sealed interface RootDestinationInstance {
    @JvmInline
    value class Authentication(val scope: AuthenticationScope) : RootDestinationInstance

    class User(val scope: ContentRootScope) : RootDestinationInstance
}

interface RootNavigator {
    fun navigateToAuthenticationHost()
    fun navigateToUser(did: AccountIdentificator.Did)
    fun navigateToWebView(to: String)
}

@Serializable
sealed interface AuthenticationSource {
    @Serializable
    data object Unspecified : AuthenticationSource

    @Serializable
    data class OAuth(val callbackUri: String) : AuthenticationSource
}

sealed interface RootScopeState {
    data object Loading : RootScopeState
    data class Error(val readableMessage: String) : RootScopeState
    data class Initialized(val scope: InitializedRootScope) : RootScopeState
}

////////////////////////////////////////////////////////////////////////////////////////////

class RootNavigatorImpl(
    private val navigator: StackNavigation<RootDestination> = StackNavigation(),
    private val onOpenWebview: (url: String) -> Unit
) : RootNavigator, StackNavigation<RootDestination> by navigator {
    override fun navigateToAuthenticationHost() {
        navigator.pushToFront(RootDestination.Authentication)
    }

    override fun navigateToUser(did: AccountIdentificator.Did) {
        navigator.pushToFront(RootDestination.User(did))
    }

    override fun navigateToWebView(to: String) {
        onOpenWebview(to)
    }
}

class InitializedRootScope(
    private val accounts: StateFlow<Map<AccountIdentificator.Did, Account>>,
    private val accountManager: AccountManager,
    private val deeplinkFlow: DeeplinkStateFlow,
    private val onOpenWebView: (String) -> Unit,
    private val rootContext: ComponentContext,
    private val logger: Logger
) {
    private val navigator = RootNavigatorImpl(onOpenWebview = onOpenWebView)

    val stack = rootContext.childStack(
        navigator,
        initialConfiguration = initialRoute(),
        handleBackButton = false,
        childFactory = ::createChild
    )

    init {
        val deeplinkHandler = RootDeeplinkHandler(
            deeplinkFlow = deeplinkFlow,
            navigator = navigator,
            stack = stack,
            logger = logger
        )

        rootContext.doOnCreate {
            rootContext.coroutineScope.launch {
                deeplinkHandler.observe()
            }
        }
    }

    private fun initialRoute() =
        if (accounts.value.isEmpty())
            RootDestination.Authentication
        else
            RootDestination.User(accounts.value.keys.first())

    private fun createChild(
        destination: RootDestination,
        componentContext: ComponentContext
    ): RootDestinationInstance =
        when (destination) {
            RootDestination.Authentication ->
                RootDestinationInstance.Authentication(
                    createAuthScope(componentContext)
                )

            is RootDestination.User ->
                createUserInstance(destination, componentContext)
        }

    private fun createAuthScope(context: ComponentContext): AuthenticationScope =
        AuthenticationScope(
            accountManager = accountManager,
            rootNavigator = navigator,
            context = context
        ).also {
            val value = deeplinkFlow.value?.value
            if (value is Deeplink.OAuthContinuation) {
                it.continueOauthAuthorization(value.rawUri)
            }
        }

    private fun createUserInstance(
        destination: RootDestination.User,
        ctx: ComponentContext
    ): RootDestinationInstance.User {
        val account = accounts.value[destination.userDid]
            ?: error("Attempted navigation for uninitialized account")

        val did = account.basicDetails.value.did
        val storage = createUserStoragePaths(did)

        return RootDestinationInstance.User(
            ContentRootScope(
                client = account.client,
                context = ctx,
                rootNavigator = navigator,
                onSoftLogout = {
                    ctx.coroutineScope.launch {
                        logger.v(tag = logTag) { "soft logout requested" }
                        accountManager.softLogout(did)
                    }
                },
                onLogout = {
                    navigator.navigateToAuthenticationHost()
                    ctx.coroutineScope.launch {
                        logger.v(tag = logTag) { "logout requested" }
                        accountManager.logout(did)
                    }
                },
                logger = logger,
                playerCacheConfiguration = VideoPlayerCacheConfiguration.Enabled(
                    path = storage.cache.resolve("v").toString(),
                    maxSize = 512.megabytes
                )
            )
        )
    }
}

private class RootDeeplinkHandler(
    private val deeplinkFlow: DeeplinkStateFlow,
    private val navigator: RootNavigatorImpl,
    private val stack: Value<ChildStack<*, RootDestinationInstance>>,
    private val logger: Logger
) {

    suspend fun observe() {
        withContext(Dispatchers.Main.immediate) {
            deeplinkFlow.filterNotNull().collect { event ->
                logger.v(tag = logTag) { "deeplink in root scope ${event.value}" }
                handle(event.value)
            }
        }
    }

    private fun handle(deeplink: Deeplink) {
        when (deeplink) {
            is Deeplink.OAuthContinuation -> {
                navigator.navigateToAuthenticationHost()

                stack
                    .findInstance<RootDestinationInstance.Authentication>()
                    ?.scope
                    ?.continueOauthAuthorization(deeplink.rawUri)
                    ?: error("Authentication scope not found")
            }

            else -> Unit
        }
    }
}


private fun createUserStoragePaths(did: AccountIdentificator.Did): StoragePaths =
    StoragePaths(
        data = PathProvider
            .userRelated(PathProvider.globalAppDataPath, did)
            .resolve("rsc"),
        cache = PathProvider
            .userRelated(PathProvider.globalCachePath, did)
            .resolve("rsc")
    )
