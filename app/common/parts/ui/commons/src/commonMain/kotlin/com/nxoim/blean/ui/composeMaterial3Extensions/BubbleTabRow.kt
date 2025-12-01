package com.nxoim.blean.ui.composeMaterial3Extensions

//import androidx.compose.animation.animateColorAsState
//import androidx.compose.animation.core.animateDpAsState
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.FlingBehavior
//import androidx.compose.foundation.gestures.ScrollableDefaults
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.lazy.LazyListScope
//import androidx.compose.foundation.lazy.LazyListState
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.LocalContentColor
//import androidx.compose.material3.LocalTextStyle
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.CompositionLocalProvider
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.drawBehind
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.unit.dp
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.launch
//
////@Composable
////fun BubbleTabRow(
////    modifier: Modifier = Modifier,
////    scrollState: LazyListState = rememberLazyListState(),
////    contentPadding: PaddingValues = PaddingValues(8.dp),
////    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
////    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
////    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
////    userScrollEnabled: Boolean = true, content: BubbleTabRowScope.() -> Unit
////) {
////    val coroutineScope = rememberCoroutineScope()
////
////    LazyRow(
////        modifier = modifier,
////        state = scrollState,
////        contentPadding = contentPadding,
////        horizontalArrangement = horizontalArrangement,
////        verticalAlignment = verticalAlignment,
////        flingBehavior = flingBehavior,
////        userScrollEnabled = userScrollEnabled
////    ) {
////        content(BubbleTabRowScope(scrollState, coroutineScope, this))
////    }
////}
////
////class BubbleTabRowScope(
////    private val scrollState: LazyListState,
////    private val coroutineScope: CoroutineScope,
////    private val lazyListScope: LazyListScope
////) : LazyListScope by lazyListScope {
////    /** Helper function so you don't have to write this@BubbleTabRowScope.Tab()
////     */
////    inline fun <T> tabsIndexed(
////        items: List<T>,
////        noinline key: ((index: Int, item: T) -> Any)? = null,
////        crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
////        crossinline itemContent: @Composable BubbleTabRowScope.(index: Int, item: T) -> Unit
////    ) = itemsIndexed(items, key, contentType) { index, stuff ->
////        this@BubbleTabRowScope.itemContent(index, stuff)
////    }
////
////    inline fun <T> LazyListScope.tabs(
////        items: List<T>,
////        noinline key: ((index: Int, item: T) -> Any)? = null,
////        crossinline selected: (index: Int, item: T) -> Boolean,
////        crossinline onSelected: (index: Int, item: T) -> Unit,
////        noinline icon: (@Composable (index: Int, item: T) -> Unit)? = null,
////        crossinline text: @Composable (index: Int, item: T) -> Unit,
////    ) = itemsIndexed(items, key) { index, stuff ->
////        this@BubbleTabRowScope.Tab(
////            selected = selected(index, stuff),
////            index = index,
////            onSelected = { onSelected(index, stuff) },
////            icon = icon?.let { { it(index, stuff) } },
////            text = { text(index, stuff) }
////        )
////    }
////
////    inline fun LazyListScope.tabs(
////        count: Int,
////        noinline selected: (index: Int) -> Boolean,
////        crossinline onSelected: (index: Int) -> Unit,
////        noinline icon: (@Composable (index: Int) -> Unit)? = null,
////        crossinline text: @Composable (index: Int) -> Unit
////    ) = items(count, key = { it }) { index ->
////        this@BubbleTabRowScope.Tab(
////            selected = selected(index),
////            index = index,
////            onSelected = { onSelected(index) },
////            icon = icon?.let { { it(index) } },
////            text = { text(index) }
////        )
////    }
////
////    @Composable
////    fun Tab(
////        selected: Boolean,
////        index: Int,
////        onSelected: () -> Unit,
////        icon: (@Composable () -> Unit)? = null,
////        text: @Composable () -> Unit,
////    ) {
////        val backgroundColor by animateColorAsState(
////            if (selected)
////                MaterialTheme.colorScheme.primaryContainer
////            else
////                MaterialTheme.colorScheme.surfaceContainer,
////            softSpring()
////        )
////
////        val contentColor by animateColorAsState(
////            if (selected)
////                MaterialTheme.colorScheme.onPrimaryContainer
////            else
////                MaterialTheme.colorScheme.onSurface,
////            softSpring()
////        )
////
////        val cornerRadius by animateDpAsState(
////            if (selected) 18.dp else 14.dp,
////            softSpring()
////        )
////
////        val clickable = remember {
////            Modifier.clickable {
////                onSelected()
////
////                coroutineScope.launch {
////                    scrollState.animateScrollToItem(index, -(scrollState.layoutInfo.viewportSize.width / 2))
////                }
////            }
////        }
////
////        Box(
////            Modifier
////                .graphicsLayer {
////                    clip = true
////                    shape = RoundedCornerShape(cornerRadius)
////                }
////                .drawBehind { drawRect(backgroundColor) }
////                .then(clickable)
////        ) {
////            Row(
////                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
////                horizontalArrangement = Arrangement.spacedBy(4.dp),
////                verticalAlignment = Alignment.CenterVertically
////            ) {
////                CompositionLocalProvider(
////                    LocalContentColor provides contentColor,
////                    LocalTextStyle provides MaterialTheme
////                        .typography
////                        .labelSmall
////                        .plus(noPaddingLineHeightStyle)
////                ) {
////                    if (icon != null) Box(Modifier.size(20.dp)) {
////                        icon()
////                    }
////
////                    text()
////                }
////            }
////        }
////    }
////}