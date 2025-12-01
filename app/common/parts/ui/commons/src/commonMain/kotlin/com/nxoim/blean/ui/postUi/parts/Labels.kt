package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.nxoim.blean.postRelatedCommons.models.LabelType
import com.nxoim.blean.postRelatedCommons.models.PostContent

@Composable
fun Labels(postStructure: PostContent) {
    Row {
        postStructure.labels?.forEach {
            when (val value = it.value) {
                LabelType.Spam -> Text("Spam")
                is LabelType.Unsupported -> Text(value.value)
                else -> {}
            }
        }
    }
}
