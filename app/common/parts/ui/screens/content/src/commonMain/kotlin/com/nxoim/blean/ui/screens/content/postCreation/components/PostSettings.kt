package com.nxoim.blean.ui.screens.content.postCreation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeUiCommons.Spacer

@Composable
fun PostSettings(modifier: Modifier = Modifier) {
    Column(modifier) {
        HorizontalDivider()
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Spacer(16.dp)
            FilterChip(
                onClick = { },
                label = { Text("Anybody can interact") },
                selected = false
            )
            Spacer(16.dp)
        }
        HorizontalDivider()
    }
}
