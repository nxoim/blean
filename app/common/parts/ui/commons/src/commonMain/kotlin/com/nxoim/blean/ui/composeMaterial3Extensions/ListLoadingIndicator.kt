package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ListLoadingIndicator(visible: Boolean, modifier: Modifier = Modifier) {
    Crossfade(visible, modifier) {
        if (it) {
            Box(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}