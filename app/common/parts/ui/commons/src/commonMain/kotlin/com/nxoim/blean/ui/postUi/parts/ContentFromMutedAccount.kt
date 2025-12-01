package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// CONTENT DISAPPEARS FROM FEED WHEN MUTED. THIS SEEMS ONLY TO
// BE RELEVANT FOR SEARCH, PROFILE BROWSING, AND QUOTES
@Composable
fun ContentFromMutedAccount() {
    OutlinedCard() {
        Text("Content from muted account", modifier = Modifier.padding(32.dp))
    }
}