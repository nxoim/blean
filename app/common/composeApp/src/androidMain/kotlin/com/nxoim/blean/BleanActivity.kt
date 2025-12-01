package com.nxoim.blean

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.retainedComponent
import com.nxoim.blean.miscelaneous.AndroidInstanceManagement
import com.nxoim.blean.miscelaneous.enableEdgeToEdgeAlwaysFullyTransparent
import com.nxoim.blean.shared.DeeplinkInstance
import com.nxoim.blean.shared.InstanceCreationReason
import com.nxoim.blean.shared.navigation.BackGestureProviderContainer
import com.nxoim.blean.ui.RootNavHost
import com.nxoim.blean.ui.RootScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val logTag = "MainActivity"

abstract class BleanActivity(
    private val creationReason: InstanceCreationReason,
    private val observeNewDeeplinks: Boolean,
    private val logger: Logger = Logger
) : ComponentActivity() {
    // needed here so onIntent can update this. this
    // wont recreate due to android:launchMode="singleTask" in manifest
    private val deeplinkFullUrisFlow = MutableStateFlow<DeeplinkInstance?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { deeplinkFullUrisFlow.value = DeeplinkInstance(it.toString()) }

        enableEdgeToEdgeAlwaysFullyTransparent()

        FileKit.init(this)

        val (rootContext, rootScope) = retainedComponent { rootContext ->
            (rootContext as DefaultComponentContext) to RootScope(
                accountManagerHolder = BleanAndroidAppInstance.accountManagerHolder,
                deeplinkFlow = deeplinkFullUrisFlow,
                instanceCreationReason = creationReason,
                context = rootContext,
                onOpenWebView = {
                    // todo custom tabs androidx.browser:browser
                    startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
                },
                logger = Logger,
                instanceManagement = AndroidInstanceManagement(this)
            )
        }

        setContent {
            BackGestureProviderContainer(rootContext) {
                Column {
//                    Row {
//                        Button(
//                            onClick = {
//                                val intent = Intent(this@MainActivity, ContentViewDeeplinkActivity::class.java)
//                                intent.flags += Intent.FLAG_ACTIVITY_MULTIPLE_TASK
//                                intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK
//                                intent.flags += Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS
//                                startActivity(intent)
//                            },
//                            modifier = Modifier.statusBarsPadding()
//                        ) {
//                            Text("Start new")
//                        }
//                    }

                    RootNavHost(rootScope)

                }
            }
        }
    }

    // android:launchMode="singleTask" in manifest too
    // we dont need onCreate to be called on mfing deeplinks omfg
    override fun onNewIntent(intent: Intent) {
        if (observeNewDeeplinks) this.lifecycleScope.launch {
            deeplinkFullUrisFlow.emit(DeeplinkInstance(intent.data.toString()))
        }

        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        logger.v(tag = logTag) { "main activity destroyed" } // needs to be before super.onDestroyed()
        super.onDestroy()
    }
}