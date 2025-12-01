package com.nxoim.blean.ui.screens.content.thread.componnents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.screens.content.thread.ThreadNavigation
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadAppBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = {
            Text("Post")
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun ThreadAppBar(
    navigation: ThreadNavigation,
    modifier: Modifier = Modifier
) = ThreadAppBar(
    onBack = navigation::back,
    modifier = modifier
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
private fun Preview() {
    MaterialExpressiveTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                ThreadAppBar(
                    onBack = { }
                )
            }
        }
    }
}