package com.nxoim.blean.ui.screens.content.thread.componnents

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ReplyButtonDivider(
    modifier: Modifier
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f))
        ReplyButton(onClick = { })
        HorizontalDivider(Modifier.weight(1f))
    }
}
