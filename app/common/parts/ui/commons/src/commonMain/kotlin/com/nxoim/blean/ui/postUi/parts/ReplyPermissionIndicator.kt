package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person4
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.nxoim.blean.postRelatedCommons.models.ReplyPermission
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ReplyPermissionIndicator(
    value: ReplyPermission,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    style: TextStyle = MaterialTheme.typography.labelLarge
) {
    val (text, icon) = remember(value) {
        when (value) {
            is ReplyPermission.Everyone ->
                "Everyone can reply" to Icons.Outlined.AccountCircle

            is ReplyPermission.NoOne ->
                "No one can reply" to Icons.Outlined.Stop

            is ReplyPermission.Restricted -> {
                val rules = value.rules

                when {
                    rules.isEmpty() -> "No one can reply" to Icons.Outlined.Stop

                    rules.size > 1 -> "Some people can reply" to Icons.Outlined.FilterList

                    else -> when (val rule = rules.first()) {
                        ReplyPermission.Restricted.Rule.Mentioned ->
                            "Only mentioned accounts can reply" to Icons.Outlined.AlternateEmail

                        ReplyPermission.Restricted.Rule.CreatorsFollows ->
                            "Only accounts followed by this account can reply" to Icons.Outlined.Group

                        ReplyPermission.Restricted.Rule.CreatorsFollowers ->
                            "Only followers can reply" to Icons.Outlined.Person4

                        is ReplyPermission.Restricted.Rule.List ->
                            "Only accounts in a list can reply" to Icons.Outlined.FilterList
                    }
                }
            }
        }
    }

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Text(
            text = text,
            style = style,
            color = color
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun Preview() {
    MaterialExpressiveTheme {
        Surface {
            ReplyPermissionIndicator(
                ReplyPermission.Everyone()
            )
        }
    }
}