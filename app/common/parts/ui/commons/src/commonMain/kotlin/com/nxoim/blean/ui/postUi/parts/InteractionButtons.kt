package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeMaterial3Extensions.MediumInteractionButton
import com.nxoim.blean.ui.composeMaterial3Extensions.MediumInteractionButtonDefaults
import com.nxoim.blean.ui.composeMaterial3Extensions.WIthSpaceFabricScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

@Composable
fun InteractionButtons(
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    isReposted: Boolean,
    onRepostQuoteClick: () -> Unit,
    onShareClick: () -> Unit,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onMoreClick: () -> Unit,
    likes: Int,
    replies: Int,
    repostsQuotes: Int
) {
    Row(
        modifier
            .fillMaxWidth()
            .alpha(0.7f),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            WIthSpaceFabricScope {
                MediumInteractionButton(
                    onClick = onLikeClick,
                    selected = isLiked,
                    label = { Text(likes.toShortenedEnglish(), maxLines = 1) },
                    colors = MediumInteractionButtonDefaults.colors
                        .copy(
                            idleSelectedBackground = MaterialTheme.colorScheme.error,
                            idleSelectedContent = MaterialTheme.colorScheme.onError,
                        ),
                    icon = {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                    },
                    shapes = MediumInteractionButtonDefaults.Shapes.start,
                )

                MediumInteractionButton(
                    onClick = onReplyClick,
                    selected = false,
                    label = { Text(replies.toShortenedEnglish(), maxLines = 1) },
                    icon = {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
                    },
                    shapes = MediumInteractionButtonDefaults.Shapes.middle,
                )

                MediumInteractionButton(
                    onClick = onRepostQuoteClick,
                    selected = isReposted,
                    label = { Text(repostsQuotes.toShortenedEnglish(), maxLines = 1) },
                    icon = {
                        Icon(Icons.Outlined.Repeat, contentDescription = null)
                    },
                    shapes = MediumInteractionButtonDefaults.Shapes.end,
                )
            }
        }

        MediumInteractionButton(
            onClick = onMoreClick,
            selected = false,
            icon = {
                Icon(Icons.Outlined.MoreVert, contentDescription = null)
            },
            shapes = MediumInteractionButtonDefaults.Shapes.single,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Int.toMagnitudeComponents(block: (trillions: Long, billions: Long, millions: Long, thousands: Long, remainder: Long) -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    var remaining = abs(this.toLong())
    val trillions = remaining / 1_000_000_000_000
    remaining %= 1_000_000_000_000
    val billions = remaining / 1_000_000_000
    remaining %= 1_000_000_000
    val millions = remaining / 1_000_000
    remaining %= 1_000_000
    val thousands = remaining / 1_000
    remaining %= 1_000

    return block(trillions, billions, millions, thousands, remaining)
}

fun Int.toShortenedEnglish(decimals: Int = 1): String =
    toMagnitudeComponents { trillions, billions, millions, thousands, remainder ->
        val negative = this < 0
        val sign = if (negative) "-" else ""

        val value: Double
        val suffix: String

        when {
            trillions > 0 -> {
                value = this / 1_000_000_000_000.0
                suffix = "t"
            }
            billions > 0 -> {
                value = this / 1_000_000_000.0
                suffix = "b"
            }
            millions > 0 -> {
                value = this / 1_000_000.0
                suffix = "m"
            }
            thousands > 0 -> {
                value = this / 1_000.0
                suffix = "k"
            }
            else -> return this.toString()
        }

        val scale = 10.0.pow(decimals)
        val rounded = round(value * scale) / scale
        val fracPart = ((rounded - floor(rounded)) * scale).toInt()
        val fracStr = if (fracPart == 0) "" else ".${fracPart.toString().padStart(decimals, '0')}"

        "$sign${floor(rounded).toInt()}$fracStr$suffix"
    }