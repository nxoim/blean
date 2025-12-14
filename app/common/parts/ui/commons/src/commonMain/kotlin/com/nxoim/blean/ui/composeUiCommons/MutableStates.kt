package com.nxoim.blean.ui.composeUiCommons


import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshots.StateFactoryMarker
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@StateFactoryMarker
fun mutableIntOffsetStateOf(initial: IntOffset): MutableState<IntOffset> {
    val longState = mutableLongStateOf(initial.packedValue)
    return object : MutableState<IntOffset> {
        override var value: IntOffset
            get() = IntOffset(longState.value)
            set(v) { longState.value = v.packedValue }

        override operator fun component1(): IntOffset = value

        override operator fun component2(): (IntOffset) -> Unit = { value = it }
    }
}

@StateFactoryMarker
fun mutableIntOffsetStateOf(x: Int, y: Int): MutableState<IntOffset> =
    mutableIntOffsetStateOf(IntOffset(x, y))

@StateFactoryMarker
fun mutableOffsetStateOf(initial: Offset): MutableState<Offset> {
    val longState = mutableLongStateOf(initial.packedValue)
    return object : MutableState<Offset> {
        override var value: Offset
            get() = Offset(longState.value)
            set(v) { longState.value = v.packedValue }

        override operator fun component1(): Offset = value

        override operator fun component2(): (Offset) -> Unit = { value = it }
    }
}

@StateFactoryMarker
fun mutableOffsetStateOf(x: Float, y: Float): MutableState<Offset> =
    mutableOffsetStateOf(Offset(x, y))

@StateFactoryMarker
fun mutableBiasAlignmentStateOf(initial: BiasAlignment): MutableState<BiasAlignment> {
    val state = mutableOffsetStateOf(initial.horizontalBias, initial.verticalBias)

    return object : MutableState<BiasAlignment> {
        override var value: BiasAlignment
            get() = BiasAlignment(state.value.x, state.value.y)
            set(v) {
                state.value =  Offset(v.horizontalBias, v.verticalBias)
            }

        override operator fun component1(): BiasAlignment = value
        override operator fun component2(): (BiasAlignment) -> Unit = { value = it }
    }
}

@StateFactoryMarker
fun mutableBiasAlignmentStateOf(horizontalBias: Float, verticalBias: Float): MutableState<BiasAlignment> =
    mutableBiasAlignmentStateOf(BiasAlignment(horizontalBias, verticalBias))

@StateFactoryMarker
fun mutableSizeStateOf(initial: Size): MutableState<Size> {
    val longState = mutableLongStateOf(initial.packedValue)
    return object : MutableState<Size> {
        override var value: Size
            get() = Size(longState.value)
            set(v) { longState.value = v.packedValue }

        override operator fun component1(): Size = value
        override operator fun component2(): (Size) -> Unit = { value = it }
    }
}

@StateFactoryMarker
fun mutableSizeStateOf(width: Float, height: Float): MutableState<Size> =
    mutableSizeStateOf(Size(width, height))

@StateFactoryMarker
fun mutableDpOffsetStateOf(initial: DpOffset): MutableState<DpOffset> {
    val longState = mutableLongStateOf(initial.packedValue)
    return object : MutableState<DpOffset> {
        override var value: DpOffset
            get() = DpOffset(longState.value)
            set(v) { longState.value = v.packedValue }

        override operator fun component1(): DpOffset = value
        override operator fun component2(): (DpOffset) -> Unit = { value = it }
    }
}

@StateFactoryMarker
fun mutableDpOffsetStateOf(x: Dp, y: Dp): MutableState<DpOffset> =
    mutableDpOffsetStateOf(DpOffset(x,y))

@StateFactoryMarker
fun mutableColorStateOf(initial: Color): MutableState<Color> {
    val longState = mutableLongStateOf(initial.value.toLong())
    return object : MutableState<Color> {
        override var value: Color
            get() = Color(longState.value.toULong())
            set(v) { longState.value = v.value.toLong() }

        override operator fun component1(): Color = value
        override operator fun component2(): (Color) -> Unit = { value = it }
    }
}

@StateFactoryMarker
fun mutableColorStateOf(red: Int, green: Int, blue: Int, alpha: Int = 255): MutableState<Color> =
    mutableColorStateOf(Color(red, green, blue, alpha))

@StateFactoryMarker
fun mutableColorStateOf(value: Long): MutableState<Color> =
    mutableColorStateOf(Color(value))

// absolutely love googles consistent api's❤️

//@StateFactoryMarker
//fun mutableIntSizeStateOf(initial: IntSize): MutableState<IntSize> {
//    val longState = mutableLongStateOf(initial.packedValue)
//    return object : MutableState<IntSize> {
//        override var value: IntSize
//            get() = androidx.compose.ui.unit.IntSize(longState.value)
//            set(v) {
//                longState.value = v.packedValue
//            }
//
//        override operator fun component1(): IntSize = value
//        override operator fun component2(): (IntSize) -> Unit = { value = it }
//    }
//}
//
//@StateFactoryMarker
//fun mutableDpSizeStateOf(initial: DpSize): MutableState<DpSize> {
//    val longState = mutableLongStateOf(initial.packedValue)
//    return object : MutableState<DpSize> {
//        override var value: DpSize
//            get() = androidx.compose.ui.unit.DpSize(longState.value)
//            set(v) { longState.value = v.packedValue }
//
//        override operator fun component1(): DpSize = value
//        override operator fun component2(): (DpSize) -> Unit = { value = it }
//    }
//}
//
//@StateFactoryMarker
//fun mutableConstraintsStateOf(initial: Constraints): MutableState<Constraints> {
//    val longState = mutableLongStateOf(initial.value)
//    return object : MutableState<Constraints> {
//        override var value: Constraints
//            get() = Constraints(longState.value)
//            set(v) { longState.value = v.value }
//
//        override operator fun component1(): Constraints = value
//        override operator fun component2(): (Constraints) -> Unit = { value = it }
//    }
//}