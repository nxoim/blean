package com.nxoim.blean.ui.screens.content.postCreation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeUiCommons.Spacer
import com.nxoim.blean.ui.screens.content.postCreation.PostWritingModel

@Composable
fun PostContentOptions(
    modifier: Modifier = Modifier,
    component: PostWritingModel,
    onResultReceived: () -> Unit
) {
    Row(modifier.horizontalScroll(rememberScrollState())) {
        Spacer(16.dp)

        PostContentOptionButton(
            onClick = {
                component.openMediaSelector(onResultReceived = { onResultReceived() })
            },
            icon = { Icon(Icons.Outlined.AccountBox, contentDescription = null) },
            label = { Text("Add media") }
        )

        Spacer(16.dp)

        PostContentOptionButton(
            onClick = {

            },
            icon = { Icon(Icons.Outlined.AccountBox, contentDescription = null) },
            label = { Text("Add GIF") }
        )

        Spacer(16.dp)

        PostContentOptionButton(
            onClick = {

            },
            icon = { Icon(Icons.Outlined.AccountBox, contentDescription = null) },
            label = { Text("Add emoji") }
        )

        Spacer(16.dp)
    }
}