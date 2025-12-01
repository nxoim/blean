package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeUiCommons.modifiers.conditionalThen
import com.nxoim.blean.ui.composeUiCommons.modifiers.noRippleClickable

/**
 * Container for modal screens, dialogs. Has a max size, and looks like a regular
 * container on smaller screens.
 */
@Composable
fun ModalContainer(
	modifier: Modifier = Modifier,
	onDismiss: () -> Unit = {},
	maxAllowedSize: DpSize = DpSize(609.dp, 806.dp),
	scrimColor: Color = MaterialTheme.colorScheme.background.copy(0.3f),
	content: @Composable BoxScope.() -> Unit
) = BoxWithConstraints(
	Modifier
		.fillMaxSize()
		.background(scrimColor)
		.noRippleClickable(onDismiss),
	contentAlignment = Alignment.Center
) {
	val canvasSize = DpSize(this.maxWidth, this.maxHeight)
	val mustApplyDecorations = canvasSize.width > maxAllowedSize.width

	Box(
		modifier
			.conditionalThen(
				condition = mustApplyDecorations,
				onTrue = {
					val shape = RoundedCornerShape(8.dp)

					it
						.widthIn(max = maxAllowedSize.width)
						.heightIn(max = maxAllowedSize.width)
						.padding(24.dp)
						.shadow(8.dp, shape)
						.clip(shape)
				}
			),
		content = content
	)
}