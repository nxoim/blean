package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ColumnScope.ReplyLineThing(modifier: Modifier = Modifier) = VerticalDivider(
    modifier
        .clip(CircleShape)
        .align(Alignment.CenterHorizontally)
        .fillMaxHeight(),
    thickness = 3.dp,
    color = MaterialTheme.colorScheme.surfaceVariant
)
