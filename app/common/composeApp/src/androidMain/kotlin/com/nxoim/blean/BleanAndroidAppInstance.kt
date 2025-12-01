package com.nxoim.blean

import BuildConfig
import android.annotation.SuppressLint
import android.app.Application
import co.touchlab.kermit.Logger
import com.nxoim.blean.client.AccountManagerHolder
import com.nxoim.blean.commonThingsDumpster.childCoroutineScope
import com.nxoim.blean.platformCredentialsManagement.AndroidCredentialsRepository
import com.nxoim.blean.shared.appEnvironment.PathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

class BleanAndroidAppInstance : Application() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val logger = Logger
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var accountManagerHolder: AccountManagerHolder
    }

    override fun onCreate() {
        super.onCreate()
        accountManagerHolder = AccountManagerHolder(
            rootDataStorageUri = PathProvider.globalAppDataPath.toString() + "/root",
            rootCacheStorageUri = PathProvider.globalCachePath.toString() + "/root",
            encryptionKeyFactory = { null },
            credentialsRepositoryFactory = {
                AndroidCredentialsRepository(
                    context = this,
                    logger,
                    coroutineScope.childCoroutineScope() + Dispatchers.IO
                )
            },
            logger = logger,
            coroutineScope = coroutineScope.childCoroutineScope(),
            baseBskyServerEndpoint = BuildConfig.Environment.defaultBskyServerEndpoint,
            baseOauthEndpoint = BuildConfig.Environment.defaultOauthEndpoint
        )
    }
}