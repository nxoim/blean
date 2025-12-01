package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// TODO simplify and consistent api and make up to date with compose 1.8 m3 app bar updates, probably rewrite
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAppBar(
    avatar: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit = { },
    maxHeight: Dp = 288.dp,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    bottomPadding: Dp = 16.dp,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIconContentColor: Color = MaterialTheme.colorScheme.onBackground,
    titleContentColor: Color = MaterialTheme.colorScheme.onBackground,
    subtitleContentColor: Color = MaterialTheme.colorScheme.outline,
    actionIconContentColor: Color = MaterialTheme.colorScheme.onBackground,
    containerColor: Color = MaterialTheme.colorScheme.background,
    scrolledContainerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
) {
    CustomCollapsibleTopAppBar(
        modifier = modifier,
        avatar = avatar,
        title = title,
        subtitle = subtitle,
        expandedTitleTextStyle = MaterialTheme.typography.titleLarge,
        collapsedTitleTextStyle = MaterialTheme.typography.titleLarge,
        subtitleTextStyle = MaterialTheme.typography.bodySmall,
        bottomPadding = bottomPadding,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        maxHeight = maxHeight,
        pinnedHeight = 64.dp,
        scrollBehavior = scrollBehavior,
        navigationIconContentColor,
        titleContentColor,
        actionIconContentColor,
        containerColor,
        scrolledContainerColor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCollapsibleTopAppBar(
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    expandedTitleTextStyle: TextStyle,
    collapsedTitleTextStyle: TextStyle,
    subtitleTextStyle: TextStyle,
    bottomPadding: Dp,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable() (RowScope.() -> Unit),
    windowInsets: WindowInsets,
    maxHeight: Dp,
    pinnedHeight: Dp,
    scrollBehavior: TopAppBarScrollBehavior?,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    actionIconContentColor: Color,
    containerColor: Color,
    scrolledContainerColor: Color
) {
    if (maxHeight <= pinnedHeight) {
        throw IllegalArgumentException(
            "A TwoRowsTopAppBar max height should be greater than its pinned height"
        )
    }
    val pinnedHeightPx: Float
    val maxHeightPx: Float
    val titleBottomPaddingPx: Int
    val statusBarPaddingPx: Float
    LocalDensity.current.run {
        pinnedHeightPx = pinnedHeight.toPx()
        maxHeightPx = maxHeight.toPx()
        titleBottomPaddingPx = bottomPadding.roundToPx()
        statusBarPaddingPx = windowInsets.asPaddingValues().calculateTopPadding().toPx()
    }

    // Sets the app bar's height offset limit to hide just the bottom title area and keep top title
    // visible when collapsed.
    SideEffect {
        if (scrollBehavior?.state?.heightOffsetLimit != pinnedHeightPx - maxHeightPx) {
            scrollBehavior?.state?.heightOffsetLimit = pinnedHeightPx - maxHeightPx
        }
    }

    // Obtain the container Color from the TopAppBarColors using the `collapsedFraction`, as the
    // bottom part of this TwoRowsTopAppBar changes color at the same rate the app bar expands or
    // collapse.
    // This will potentially animate or interpolate a transition between the container color and the
    // container's scrolled color according to the app bar's scroll state.
    val collapseFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    val appBarContainerColor by animateColorAsState(
        if ((scrollBehavior?.state?.contentOffset ?: 0f) > -1f)
            containerColor
        else
            scrolledContainerColor,
        label = ""
    )
    // Wrap the given actions in a Row.
    val actionsRow = @Composable {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }

    // Hide the top row title semantics when its alpha value goes below 0.5 threshold.
    // Hide the bottom row title semantics when the top title semantics are active.
    val hideSubtitleSemantics = collapseFraction < 0.5f

    // Set up support for resizing the top app bar when vertically dragging the bar itself.
    val appBarDragModifier = if (scrollBehavior != null && !scrollBehavior.isPinned) {
        Modifier.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffset + delta
            },
            onDragStopped = { velocity ->
                settleAppBar(
                    scrollBehavior.state,
                    velocity,
                    scrollBehavior.flingAnimationSpec,
                    scrollBehavior.snapAnimationSpec
                )
            }
        )
    } else {
        Modifier
    }

    Surface(modifier = modifier.then(appBarDragModifier), color = appBarContainerColor) {
        CustomCollapsibleTopAppBarLayout(
            modifier = Modifier
                // only apply the horizontal sides of the window insets padding, since the top
                // padding will always be applied by the layout above
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                .clipToBounds(),
            maxHeight = maxHeightPx,
            pinnedHeightPx = pinnedHeightPx,
            statusBarPaddingPx = statusBarPaddingPx,
            navigationIconContentColor = navigationIconContentColor,
            titleContentColor = titleContentColor,
            actionIconContentColor = actionIconContentColor,
            avatar = avatar,
            title = title,
            subtitle = subtitle,
            expandedTitleTextStyle = expandedTitleTextStyle,
            collapsedTitleTextStyle = collapsedTitleTextStyle,
            subtitleTextStyle = subtitleTextStyle,
            collapseFraction = collapseFraction,
            titleBottomPadding = titleBottomPaddingPx,
            hideSubtitleSemantics = hideSubtitleSemantics,
            navigationIcon = navigationIcon,
            actions = actionsRow
        )
    }
}

@Composable
private fun CustomCollapsibleTopAppBarLayout(
    modifier: Modifier,
    maxHeight: Float,
    pinnedHeightPx: Float,
    statusBarPaddingPx: Float,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    actionIconContentColor: Color,
    avatar: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    expandedTitleTextStyle: TextStyle,
    collapsedTitleTextStyle: TextStyle,
    subtitleTextStyle: TextStyle,
    collapseFraction: Float,
    titleBottomPadding: Int,
    hideSubtitleSemantics: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Layout(
        {
            Box(
                Modifier
                    .layoutId("navigationIcon")
                    .padding(start = TopAppBarHorizontalPadding)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconContentColor,
                    content = navigationIcon
                )
            }

            Box(Modifier.layoutId("avatar")) { avatar() }

            Box(
                Modifier
                    .layoutId("title")
                    .padding(horizontal = TopAppBarHorizontalPadding)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides titleContentColor,
                    LocalTextStyle provides MaterialTheme.typography.titleLarge,
                    content = title
                )
            }

            Box(
                Modifier
                    .layoutId("subtitle")
                    .padding(horizontal = TopAppBarHorizontalPadding)
                    .then(if (hideSubtitleSemantics) Modifier.clearAndSetSemantics { } else Modifier)
                    .graphicsLayer { alpha = (1f - collapseFraction * 2) }
            ) {
                ProvideTextStyle(value = subtitleTextStyle) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.outline,
                        content = subtitle
                    )
                }
            }

            Box(
                Modifier
                    .layoutId("actionIcons")
                    .padding(end = TopAppBarHorizontalPadding)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions
                )
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val subtitleIsRendered = collapseFraction < 0.5f
        val minAvatarSizePx = 42.dp.toPx()
        val maxAvatarSizePx = 208.dp.toPx()
        val paddingBetweenTextAndAvatar = 16.dp.toPx()

        val navigationIconPlaceable = measurables
            .first { it.layoutId == "navigationIcon" }
            .measure(constraints.copy(minWidth = 0))

        val actionIconsPlaceable = measurables
            .first { it.layoutId == "actionIcons" }
            .measure(constraints.copy(minWidth = 0))

        val maxTitleWidth = if (constraints.maxWidth == Constraints.Infinity) {
            constraints.maxWidth
        } else {
            (constraints.maxWidth - navigationIconPlaceable.width - actionIconsPlaceable.width)
                .coerceAtLeast(0)
        }

        val titlePlaceable = measurables
            .first { it.layoutId == "title" }
            .measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        val subtitlePlaceable = measurables
            .first { it.layoutId == "subtitle" }
            .measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        val avatarPlaceable = measurables
            .first { it.layoutId == "avatar" }
            .measure(
                constraints.copy(
                    minWidth = 0,
                    maxWidth = (maxAvatarSizePx * (1f - collapseFraction) + (minAvatarSizePx * collapseFraction)).roundToInt()
                )
            )

        val currentLayoutHeight = ((maxHeight * (1f - collapseFraction)) +
                (pinnedHeightPx  * collapseFraction) + statusBarPaddingPx).roundToInt()

        val avatarCollapsedPosition = IntOffset(
            max(TopAppBarTitleInset.roundToPx(), navigationIconPlaceable.width),
            (statusBarPaddingPx + (pinnedHeightPx - avatarPlaceable.height) / 2).roundToInt()
        )

        val titleCollapsedPosition = IntOffset(
            max(TopAppBarTitleInset.roundToPx(), (avatarCollapsedPosition.x + avatarPlaceable.width + (8 * density)).roundToInt()),
            (statusBarPaddingPx + (pinnedHeightPx - titlePlaceable.height) / 2).roundToInt()
        )

        val titleExpandedPosition = IntOffset(
            (constraints.maxWidth - titlePlaceable.width) / 2,
            (statusBarPaddingPx + (pinnedHeightPx - titlePlaceable.height - subtitlePlaceable.height) / 2).roundToInt()
        )

        val avatarExpandedPosition = IntOffset(
            (constraints.maxWidth - avatarPlaceable.width) / 2,
            paddingBetweenTextAndAvatar.roundToInt() + titleExpandedPosition.y + titlePlaceable.height + subtitlePlaceable.height
        )

        layout(constraints.maxWidth, currentLayoutHeight) {
            val titleOffset = IntOffset(
                x = ((titleCollapsedPosition.x * collapseFraction) + (titleExpandedPosition.x * (1f - collapseFraction))).roundToInt(),
                y = ((titleCollapsedPosition.y * collapseFraction) + (titleExpandedPosition.y * (1f - collapseFraction))).roundToInt()
            )
            val subtitleOffset = IntOffset(
                x = (constraints.maxWidth - subtitlePlaceable.width) / 2,
                y = titleOffset.y + titlePlaceable.height
            )

            val avatarOffset = IntOffset(
                x = ((avatarCollapsedPosition.x * collapseFraction) + (avatarExpandedPosition.x * (1f - collapseFraction))).roundToInt(),
                y = ((avatarCollapsedPosition.y * collapseFraction) + (avatarExpandedPosition.y * (1f - collapseFraction))).roundToInt()
            )

            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = (statusBarPaddingPx + (pinnedHeightPx - navigationIconPlaceable.height) / 2).roundToInt()
            )

            avatarPlaceable.placeRelative(avatarOffset)

            // Title
            titlePlaceable.placeRelative(titleOffset)

            if (subtitleIsRendered) subtitlePlaceable.placeRelative(subtitleOffset)

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width,
                y = (statusBarPaddingPx + (pinnedHeightPx - actionIconsPlaceable.height) / 2).roundToInt()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleAppBar(
    state: TopAppBarState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?
): Velocity {
    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
        return Velocity.Zero
    }
    var remainingVelocity = velocity
    // In case there is an initial velocity that was left after a previous user fling, animate to
    // continue the motion to expand or collapse the app bar.
    if (flingAnimationSpec != null && abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(
            initialValue = 0f,
            initialVelocity = velocity,
        )
            .animateDecay(flingAnimationSpec) {
                val delta = value - lastValue
                val initialHeightOffset = state.heightOffset
                state.heightOffset = initialHeightOffset + delta
                val consumed = abs(initialHeightOffset - state.heightOffset)
                lastValue = value
                remainingVelocity = this.velocity
                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }
    }
    // Snap if animation specs were provided.
    if (snapAnimationSpec != null) {
        if (state.heightOffset < 0 &&
            state.heightOffset > state.heightOffsetLimit
        ) {
            AnimationState(initialValue = state.heightOffset).animateTo(
                if (state.collapsedFraction < 0.5f) {
                    0f
                } else {
                    state.heightOffsetLimit
                },
                animationSpec = snapAnimationSpec
            ) { state.heightOffset = value }
        }
    }

    return Velocity(0f, remainingVelocity)
}

private val TopAppBarHorizontalPadding = 4.dp
private val TopAppBarTitleInset = 16.dp - TopAppBarHorizontalPadding