package com.nxoim.blean.ui.screens.content.postCreation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeMaterial3Extensions.ExpressiveBoundsTransform
import com.nxoim.blean.ui.composeUiCommons.Spacer
import com.nxoim.blean.ui.composeUiCommons.copy
import com.nxoim.blean.ui.screens.content.postCreation.PostWritingModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
context(animatedVisibilityScope: AnimatedVisibilityScope, transitionScope: SharedTransitionScope)
fun PostWritingScaffold(
    model: PostWritingModel,
    sharedContentState: SharedTransitionScope.SharedContentState,
    onHide: () -> Unit
) {
    val textFieldFocusRequester = remember { FocusRequester() }

    with(transitionScope) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.sharedBounds(
                    sharedContentState,
                    animatedVisibilityScope,
                    boundsTransform = ExpressiveBoundsTransform
                ),
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    onHide()
                                    model.saveCurrentToDraftAndReset()
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                            }
                        },
                        actions = {
                            Row(
                                Modifier.padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val characters = model.currentWritingSession.text.length
                                Text(
                                    characters.toString() + "/${model.maxCharacters}",
                                    color = if (characters != model.maxCharacters)
                                        MaterialTheme.colorScheme.outline
                                    else
                                        MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(8.dp)
                                Crossfade(model.currentWritingSession.isNotEmpty()) { sessionNotEmpty ->
                                    Button(
                                        onClick = {
                                            model.uploadPostAndReset(
                                                model.currentWritingSession
                                            )
                                            onHide()
                                        },
                                        enabled = sessionNotEmpty
                                    ) {
                                        Icon(
                                            Icons.Outlined.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(8.dp)
                                        Text("Post")
                                    }
                                }
                            }
                        }
                    )
                }
            ) { scaffoldPadding ->
                PostWritingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding.copy(bottom = 0.dp)),
                    model,
                    textFieldFocusRequester
                )
            }
        }
    }
}