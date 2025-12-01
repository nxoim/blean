package com.nxoim.blean

import BuildConfig
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.instancekeeper.retainedSimpleInstance
import com.github.michaelbull.result.getOrThrow
import com.nxoim.blean.client.AccountManagerHolder
import com.nxoim.blean.commonThingsDumpster.childCoroutineScope
import com.nxoim.blean.platformCredentialsManagement.IOSCredentialsRepository
import com.nxoim.blean.platformCredentialsManagement.IOSEncryptionKeyManager
import com.nxoim.blean.platformCredentialsManagement.KeyType
import com.nxoim.blean.platformCredentialsManagement.getOrCreate
import com.nxoim.blean.shared.InstanceCreationReason
import com.nxoim.blean.shared.PlatformInstanceManagement
import com.nxoim.blean.shared.navigation.BackGestureProviderContainer
import com.nxoim.blean.ui.RootNavHost
import com.nxoim.blean.ui.RootScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import platform.UIKit.UIViewController

val logger = Logger.apply {
//    addLogWriter(
//        object : LogWriter() {
//            override fun log(
//                severity: Severity,
//                message: String,
//                tag: String,
//                throwable: Throwable?
//            ) {
//                println(
//                    "$tag: $message ${
//                        throwable?.stackTraceToString()?.run { "\n$this" } ?: ""
//                    }")
//            }
//
//        }
//    )
}

@OptIn(ExperimentalDecomposeApi::class)
fun MainViewController(
    dataStoragePath: String,
    cacheStoragePath: String,
    defaultComponentContext: DefaultComponentContext,
    keyManager: IOSEncryptionKeyManager,
    instanceManagement: PlatformInstanceManagement,
): UIViewController {
    if (accountManagerHolder == null) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        accountManagerHolder = AccountManagerHolder(
            rootDataStorageUri = "$dataStoragePath/root",
            rootCacheStorageUri = "$cacheStoragePath/root",
            encryptionKeyFactory = { null },
            credentialsRepositoryFactory = {
                IOSCredentialsRepository(
                    keyManager.getOrCreate("3", KeyType.RandomBytes(64)).getOrThrow(),
                    "${dataStoragePath}/b",
                    logger = logger
                )
            },
            logger = logger,
            coroutineScope = scope.childCoroutineScope(),
            baseBskyServerEndpoint = BuildConfig.Environment.defaultBskyServerEndpoint,
            baseOauthEndpoint = BuildConfig.Environment.defaultOauthEndpoint
        )
    }

    return ComposeUIViewController {
        val uriHandler = LocalUriHandler.current
        val rootScope = defaultComponentContext.retainedSimpleInstance {
            RootScope(
                accountManagerHolder = accountManagerHolder!!,
                deeplinkFlow = MutableStateFlow(null),
                instanceCreationReason = InstanceCreationReason.RegularLaunchOrResume,
                context = defaultComponentContext,
                onOpenWebView = {
                    uriHandler.openUri(it)
                },
                logger = logger,
                instanceManagement = instanceManagement
            )
        }
        BackGestureProviderContainer(defaultComponentContext) {
            RootNavHost(rootScope)
        }
    }
}

private var accountManagerHolder: AccountManagerHolder? = null

