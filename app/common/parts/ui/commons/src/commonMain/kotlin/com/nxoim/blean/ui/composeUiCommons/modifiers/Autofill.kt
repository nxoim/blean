@file:OptIn(ExperimentalComposeUiApi::class)

package com.nxoim.blean.ui.composeUiCommons.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

// todo probably should remove
fun Modifier.autofill(autofillManager: AutofillManager) = this
	.onGloballyPositioned { autofillManager.updateUiBounds(it.boundsInWindow()) }
	.onFocusChanged { focusState ->
		if (focusState.isFocused)
			autofillManager.requestAutofillForNode()
		else
			autofillManager.cancelAutofillForNode()
	}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun autofillManager(vararg types: AutofillType, onAutofill: (String) -> Unit): AutofillManager {
	val autofill = LocalAutofill.current
	val autofillTree = LocalAutofillTree.current
	val node = remember { AutofillNode(types.toList(), onFill = onAutofill) }
	autofillTree += node

	return remember {
		AutofillManager(
			updateUiBounds = { node.boundingBox = it },
			requestAutofillForNode = { autofill?.requestAutofillForNode(node) },
			cancelAutofillForNode = { autofill?.cancelAutofillForNode(node) }
		)
	}
}

class AutofillManager(
	val updateUiBounds: (Rect) -> Unit,
	val requestAutofillForNode: () -> Unit,
	val cancelAutofillForNode: () -> Unit
)