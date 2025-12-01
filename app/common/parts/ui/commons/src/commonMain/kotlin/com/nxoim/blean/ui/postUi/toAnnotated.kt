package com.nxoim.blean.ui.postUi

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.nxoim.blean.postRelatedCommons.models.TextAndFacets
import com.nxoim.blean.postRelatedCommons.models.TextFacet

@Composable
fun TextAndFacets.toAnnotated(
    linkColor: Color = MaterialTheme.colorScheme.tertiary
): AnnotatedString = remember {
    runCatching { toAnnotatedString(linkColor) }
        .getOrDefault(AnnotatedString("ERROR ANNOTATING STRING. LIKELY REVERSED RANGE SKILL ISSUE"))

}

fun TextAndFacets.toAnnotatedString(linkColor: Color) = buildAnnotatedString {
    append(this@toAnnotatedString.text)

    facets
        ?.forEach { facet ->
            when (facet) {
                is TextFacet.Link -> {
                    addLink(
                        LinkAnnotation.Url(facet.url),
                        start = facet.firstCharIndex,
                        end = facet.lastCharIndex
                    )
                    addStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = facet.firstCharIndex,
                        end = facet.lastCharIndex
                    )
                }
                is TextFacet.Did -> {
                    addStringAnnotation(
                        tag = "mention",
                        annotation = facet.did.toString(),
                        start = facet.firstCharIndex,
                        end = facet.lastCharIndex
                    )
                    addStyle(
                        SpanStyle(color = linkColor),
                        start = facet.firstCharIndex,
                        end = facet.lastCharIndex
                    )
                }
                is TextFacet.Tag -> {
                    addStringAnnotation(
                        tag = "hashtag",
                        annotation = facet.tag,
                        start = facet.firstCharIndex,
                        end = facet.lastCharIndex
                    )
                    addStyle(
                        SpanStyle(color = linkColor),
                        start = facet.firstCharIndex,
                        end = facet.lastCharIndex
                    )
                }
            }
        }
}