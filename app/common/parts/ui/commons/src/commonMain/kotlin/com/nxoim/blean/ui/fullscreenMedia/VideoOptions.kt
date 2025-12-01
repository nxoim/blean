package com.nxoim.blean.ui.fullscreenMedia

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoOptionsContainer(
    modifier: Modifier = Modifier,
    containerShape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp),
    content: @Composable context(VideoOptionsScope) () -> Unit
) {
    Surface(
        color = containerColor,
        shape = containerShape,
        modifier = modifier
    ) {
        Column(
            Modifier
                .padding(contentPadding),
            verticalArrangement = spacedBy(12.dp)
        ) {
            content(VideoOptionsScope)
        }
    }
}


object VideoOptionsScope

private val speeds by lazy {
    listOf(
        0.2f,
        0.5f,
//        0.75f,
        1f,
//        1.25f,
        1.5f,
//        1.75f,
        2f
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
context(_: VideoOptionsScope)
fun SpeedSelector(
    currentSpeedMultiplier: Float,
    onNewSpeedMultiplier: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        speeds.fastForEachIndexed { index, speed ->
            ToggleButton(
                checked = speed == currentSpeedMultiplier,
                onCheckedChange = { onNewSpeedMultiplier(speed) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    speeds.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                modifier = Modifier.weight(1f)
            ) {
                Text("${speed}x")
            }
        }
    }
}

@Composable
context(_: VideoOptionsScope)
fun Setting(
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable () -> Unit = { },
    onLongClick: () -> Unit = { },
    onDoubleClick: () -> Unit = { }
) {
    Row(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onDoubleClick = onDoubleClick
            )
            .fillMaxWidth(),
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon()

        Column {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleMedium
            ) {
                title()
            }

            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelMedium,
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                subtitle()
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            VideoOptionsContainer(Modifier.padding(16.dp)) {
                SpeedSelector(
                    1f,
                    { },
                    Modifier.padding(horizontal = 16.dp)
                )

                Column(
                    verticalArrangement = spacedBy(4.dp)
                ) {
                    Setting(
                        title = { Text("Subtitles") },
                        subtitle = { Text("English") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.ShortText,
                                contentDescription = null,
                            )
                        },
                        onClick = { },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
