package com.nxoim.blean.ui.screens.authentication.login.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeMaterial3Extensions.BigButton
import com.nxoim.blean.ui.screens.authentication.login.LoginModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSignInScreen(model: LoginModel) {
    var isDropDownVisible by rememberSaveable { mutableStateOf(false) }
    var isServerDialogueSelectionVisible by rememberSaveable { mutableStateOf(false) }

    if (isServerDialogueSelectionVisible) ServerUrlChangeDialog(
        defaultUrl = "https://bsky.social", // todo to a separate variable
        selectedUrl = model.serverUrl,
        onDismissRequest = { isServerDialogueSelectionVisible = false },
        onCustomServerUrlConfirmed = {
            model.serverUrl = it
            isServerDialogueSelectionVisible = false
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { },
            actions = {
                IconButton(onClick = { isDropDownVisible = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)

                    // stays in the icon to inherit it's position on screen
                    DropdownMenu(
                        isDropDownVisible,
                        onDismissRequest = { isDropDownVisible = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change hosting provider") },
                            leadingIcon = {
                                // TODO icons default language? or whatever
                            },
                            onClick = {
                                isDropDownVisible = false
                                isServerDialogueSelectionVisible = true
                            }
                        )
                    }
                }
            }
        )

        Column(
            Modifier.fillMaxHeight().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
        ) {
            Text("Welcome to App Name", style = MaterialTheme.typography.headlineLarge)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BigButton(onClick = model::attemptLogin) {
                    // todo icon for entering
                    Icon(Icons.Outlined.Warning, null, Modifier.size(24.dp))
                    Text("Sign in or create an account")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val textStyle = MaterialTheme.typography.labelSmall
                    val linkColor = MaterialTheme.colorScheme.tertiary
                    val signInDetailsText = remember(key1 = model.serverUrl) {
                        buildAnnotatedString {
                            append("Signing in on ")

                            withStyle(textStyle.toSpanStyle().copy(color = linkColor)) {
                                append(model.serverUrl)
                            }

                            append(". A web page will be opened in an\nin-app browser.")
                        }
                    }

                    val tosText = remember {
                        buildAnnotatedString {
                            append("By pressing “Sign in or create an account” you agree to App Name’s\n")

                            val linkStyle = textStyle
                                .toSpanStyle()
                                .copy(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )

                            withStyle(linkStyle) {
                                append("Terms and Conditions")
                            }

                            append(".")
                        }
                    }

                    Text(
                        signInDetailsText,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        tosText,
                        style = textStyle,
                        modifier = Modifier.clickable(onClick = { TODO("TOS") }),
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
