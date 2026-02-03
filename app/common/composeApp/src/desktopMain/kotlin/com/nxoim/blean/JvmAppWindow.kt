package com.nxoim.blean

import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File

fun jvmAppWindow(
    name: String = "blean unknown"
) {

    application {

        Window(
            onCloseRequest = ::exitApplication,
            title = name
        ) {
            Text("Not implemented")
        }
    }
}

private val rootUserDataFolder = File(System.getProperty("user.home") + "/.blean").absolutePath