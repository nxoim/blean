package com.nxoim.blean.ui.screens.content.postCreation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import com.nxoim.blean.ui.screens.content.postCreation.WritingSession

@Composable
fun WritingSection(
    modifier: Modifier = Modifier,
    draftsExist: Boolean,
    writingSession: WritingSession,
    textFieldFocusRequester: FocusRequester
) {
    val materialTypography = MaterialTheme.typography

    val textSuggestion = if (draftsExist)
        "Write something, or \nrestore from drafts"
    else
        "Write something"

    Box(modifier.height(IntrinsicSize.Max)) {
        // todo maybe more advanced text measurement
        val fontStyle by remember {
            derivedStateOf {
                if (writingSession.text.length < 150)
                    materialTypography.headlineMedium
                else
                    materialTypography.titleLarge
            }
        }

        if (writingSession.text.isEmpty()) {
            Text(
                textSuggestion,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        BasicTextField(
            writingSession.text,
            onValueChange = {
                writingSession.updateText(it)
            },
            textStyle = fontStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.focusRequester(textFieldFocusRequester),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}