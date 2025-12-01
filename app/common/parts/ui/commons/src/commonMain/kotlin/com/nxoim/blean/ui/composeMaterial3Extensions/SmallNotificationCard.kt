package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This is supposed to be single line only
 */
@Composable
fun SmallNotificationCard(
	modifier: Modifier = Modifier,
	icon: @Composable (() -> Unit)? = null,
	notificationContent: @Composable BoxScope.() -> Unit,
	buttonContent: @Composable RowScope.() -> Unit,
) {
	Box(
		modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(32.dp))
			.background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
		contentAlignment = Alignment.CenterStart
	) {
		Row(
			Modifier
				.fillMaxWidth()
				.padding(
					start = if (icon != null) 8.dp else 16.dp,
					end = 8.dp,
					top = 8.dp,
					bottom = 8.dp
				),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Row(
				Modifier.weight(1f, true).fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				icon?.let {
					CompositionLocalProvider(
						LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer
					) {
						Row(verticalAlignment = Alignment.CenterVertically) {
							Box(
								Modifier
									.size(32.dp)
									.clip(CircleShape)
									.background(MaterialTheme.colorScheme.primaryContainer),
								contentAlignment = Alignment.Center
							) {
								Box(Modifier.size(16.dp)) {
									it()
								}
							}

							Spacer(modifier = Modifier.width(16.dp))
						}
					}
				}

				CompositionLocalProvider(
					LocalTextStyle provides MaterialTheme.typography.bodyMedium,
					LocalContentColor provides MaterialTheme.colorScheme.primary
				) {
					Box(content = notificationContent)
				}
			}

			buttonContent()
		}
	}
}

@Composable
fun RowScope.SmallNotificationButton(modifier: Modifier, content: @Composable BoxScope.() -> Unit) = Box(
	Modifier
		.weight(1f, false)
		.clip(CircleShape)
		.then(modifier)
		.background(
			if (isSystemInDarkTheme())
				MaterialTheme.colorScheme.surfaceColorAtElevation(60.dp)
			else
				MaterialTheme.colorScheme.surfaceColorAtElevation(40.dp)
		)
) {
	CompositionLocalProvider(
		LocalTextStyle provides TextStyle(
			fontSize = 12.sp,
			fontWeight = FontWeight.W500,
		),
		LocalContentColor provides MaterialTheme.colorScheme.primary
	) {
		Box(
			Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
			content = content
		)
	}
}