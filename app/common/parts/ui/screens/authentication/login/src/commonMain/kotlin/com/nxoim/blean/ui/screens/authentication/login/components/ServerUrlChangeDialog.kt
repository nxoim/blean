package com.nxoim.blean.ui.screens.authentication.login.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nxoim.blean.ui.composeMaterial3Extensions.SettingsItem

@Composable
fun ServerUrlChangeDialog(
    defaultUrl: String,
    selectedUrl: String,
    onDismissRequest: () -> Unit,
    onCustomServerUrlConfirmed: (String) -> Unit
) {
    var isDefaultSelected by rememberSaveable {
        mutableStateOf(defaultUrl == selectedUrl)
    }
    var newSelectedUrl by rememberSaveable { mutableStateOf(selectedUrl) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Choose your hosting provider")
        },
        text = {
            Column {
                SettingsItem(
                    onClick = { isDefaultSelected = true },
                    leadingIcon = {
                        RadioButton(
                            selected = isDefaultSelected,
                            onClick = { isDefaultSelected = true }
                        )
                    },
                    title = { Text("Bluesky Social") },
                    text = { Text("Official Bluesky hosting") }
                )

                SettingsItem(
                    onClick = { isDefaultSelected = false },
                    leadingIcon = {
                        RadioButton(
                            selected = !isDefaultSelected,
                            onClick = { isDefaultSelected = false }
                        )
                    },
                    title = { Text("Custom") },
                )

                AnimatedVisibility(!isDefaultSelected) {
                    OutlinedTextField(
                        newSelectedUrl,
                        onValueChange = { newSelectedUrl = it },
                        label = { Text("Hosting provider URL") }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val url = if (isDefaultSelected) defaultUrl else newSelectedUrl

                    onCustomServerUrlConfirmed(url)
                }
            ) {
                Text("Save")
            }
        }
    )
}
