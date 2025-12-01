package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
private fun SettingsItemPreview() {
    Column(
        Modifier
            .width(380.dp)
            .height(600.dp)
            .border(1.dp, Color.Blue)
            .padding(16.dp)
    ) {
        SettingsItem(
            onClick = { },
            leadingIcon =  {
                RadioButton(
                    selected = true,
                    onClick = { }
                )
            },
            title = { Text("Title") },
            text = { Text("Text ablablablabla")},
            trainingIcon = {
                Switch(
                    checked = true,
                    onCheckedChange = { _ -> }
                )
            }
        )
    }

}

@Composable
fun SettingsItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trainingIcon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    text: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon?.invoke()
            }

            SettingText(title, text)
        }


        trainingIcon?.invoke()
    }
}

@Composable
@Preview
private fun SettingTextPreview() {
    CompositionLocalProvider(
        LocalDensity provides Density(2f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = true,
                onClick = { }
            )

            SettingText(
                title = { Text("Title") },
                text = { Text("Text ablablablabla")}
            )
        }
    }
}

@Composable
fun SettingText(
    title: @Composable () -> Unit,
    text: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge
        ) {
            title()
        }

        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            LocalContentColor provides MaterialTheme.colorScheme.outline
        ) {
            text?.invoke()
        }
    }
}