package com.nxoim.blean.ui.composeUiCommons.modifiers

import androidx.compose.ui.Modifier

inline fun Modifier.conditionalThen(
	condition: Boolean,
	onTrue: (Modifier) -> Modifier = { it },
	onFalse: (Modifier) -> Modifier = { it },
): Modifier = then(if (condition) onTrue(Modifier) else onFalse(Modifier))