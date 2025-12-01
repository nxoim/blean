package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxoim.blean.ui.composeUiCommons.Layout
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenericMediaError(
    onClick: () -> Unit,
    text: @Composable () -> Unit = {
        Text(
            "An error occurred when loading"
        )
    },
    details: (@Composable () -> Unit)? = null,
    hint: @Composable () -> Unit = {
        Text("Tap to retry")
    },
    icon: @Composable () -> Unit = {
        Text("\uD83D\uDE13", fontSize = 32.sp)
    },
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
) {
    Surface(
        shape = shape,
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            Modifier
                .width(IntrinsicSize.Max)
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            icon()

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleLarge
                ) {
                    text()
                }
            }

            details?.invoke()

            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium
            ) {
                hint()
            }
        }
    }
}

@Composable
fun GenericMediaErrorDetailsContainer(
    details: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.error,
        shape = MaterialTheme.shapes.small,
    ) {
        // NOTE: add copy error on long tap?
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .heightIn(max = 150.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelMedium,
                LocalContentColor provides MaterialTheme.colorScheme.onError
            ) {
                Layout(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) { details() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun Preview() {
    MaterialExpressiveTheme {
        Surface {
            GenericMediaError(
                onClick = {},
                details = {
                    GenericMediaErrorDetailsContainer() {
                        Text("Error details")
                    }
                },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .size(400.dp, 200.dp)
                    .padding(16.dp)
            )
        }
    }
}