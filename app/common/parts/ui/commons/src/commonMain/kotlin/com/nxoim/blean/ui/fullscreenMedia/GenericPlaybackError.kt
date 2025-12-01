package com.nxoim.blean.ui.fullscreenMedia

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeMaterial3Extensions.GenericMediaError
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GenericPlaybackError(
    onReloadRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    GenericMediaError(
        onClick = onReloadRequest,
        text = {
            Text(
                "An issue occurred while playing the video",
                textAlign = TextAlign.Center
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun Preview() {
    MaterialExpressiveTheme {
        Surface {
            GenericPlaybackError(
                onReloadRequest = {},
                modifier = Modifier.padding(16.dp).height(300.dp)
            )
        }
    }
}