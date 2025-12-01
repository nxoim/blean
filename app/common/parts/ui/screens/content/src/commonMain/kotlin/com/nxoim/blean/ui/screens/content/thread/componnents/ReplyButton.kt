package com.nxoim.blean.ui.screens.content.thread.componnents

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeUiCommons.Spacer
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ReplyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick,
        modifier = modifier
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardReturn,
            contentDescription = null,
            Modifier.size(20.dp)
        )

        Spacer(4.dp)
        Text("Tap to reply")
    }

}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun Preview() {
    MaterialExpressiveTheme {
        Surface {
            ReplyButton({}, modifier = Modifier.padding(16.dp))
        }
    }
}
