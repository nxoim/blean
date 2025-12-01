package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class SpaceFabricScope {
    private var key by mutableStateOf<Any?>(null)
    private val registeredItemsInOrder = mutableStateSetOf<CompositeKeyHashCode>()
    var epicentre by mutableIntStateOf(-1)
        private set
    val epicentreExists get() = epicentre != -1

    fun reregisterAll(key: Any?) {
        registeredItemsInOrder.clear()
        this.key = key
    }

    @Composable
    fun registerAndRememberItemState(): SpaceFabricItem {
        val key = currentCompositeKeyHashCode
        return remember(key) {
            registeredItemsInOrder.add(key)

            val index = registeredItemsInOrder.size - 1

            SpaceFabricItem(
                press = { epicentre = index },
                release = { epicentre = -1 },
                _isEpicentre = { epicentre == index },
                _distanceFromEpicentre = { // left right represented by negativity
                    if (epicentreExists) {
                        (epicentre - index).toFloat()
                    } else {
                        0f
                    }
                }
            )
        }
    }
}

class SpaceFabricItem(
    val press: () -> Unit,
    val release: () -> Unit,
    private val _isEpicentre: () -> Boolean,
    private val _distanceFromEpicentre: () -> Float
) {
    val isEpicentre get() = _isEpicentre()
    val distanceFromEpicentre get() = _distanceFromEpicentre()
}

val LocalSpaceFabricScope = staticCompositionLocalOf<SpaceFabricScope?> { null }

@Composable
fun rememberSpaceFabricScope(key: Any? = null) = remember { SpaceFabricScope() }
    .apply { remember(key) { reregisterAll(key) } }

@Composable
fun WIthSpaceFabricScope(
    scope: SpaceFabricScope = rememberSpaceFabricScope(),
    content: @Composable SpaceFabricScope.() -> Unit
) = CompositionLocalProvider(
    LocalSpaceFabricScope provides scope
) {
    scope.content()
}