package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

@Stable
fun logarithmicGradient(
    vararg colorStops: Pair<Float, Color>,
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
    base: Float = 10.0f,
    tileMode: TileMode = TileMode.Clamp,
): Brush {
    val colors = List(colorStops.size) { i -> colorStops[i].second }
    val stops = List(colorStops.size) { i -> colorStops[i].first }
    return LogarithmicGradient(colors = colors, stops = stops, start = start, end = end, base = base, tileMode = tileMode)
}

@Stable
fun logarithmicGradient(
    colors: List<Color>,
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
    base: Float = 10.0f,
    tileMode: TileMode = TileMode.Clamp,
): Brush =
    LogarithmicGradient(colors = colors, stops = null, start = start, end = end, base = base, tileMode = tileMode)

@Stable
fun horizontalLogarithmicGradient(
    colors: List<Color>,
    startX: Float = 0.0f,
    endX: Float = Float.POSITIVE_INFINITY,
    base: Float = 10.0f,
    tileMode: TileMode = TileMode.Clamp,
): Brush = logarithmicGradient(colors, Offset(startX, 0.0f), Offset(endX, 0.0f), base, tileMode)

@Stable
fun horizontalLogarithmicGradient(
    vararg colorStops: Pair<Float, Color>,
    startX: Float = 0.0f,
    endX: Float = Float.POSITIVE_INFINITY,
    base: Float = 10.0f,
    tileMode: TileMode = TileMode.Clamp,
): Brush =
    logarithmicGradient(
        *colorStops,
        start = Offset(startX, 0.0f),
        end = Offset(endX, 0.0f),
        base = base,
        tileMode = tileMode,
    )

@Stable
fun verticalLogarithmicGradient(
    colors: List<Color>,
    startY: Float = 0.0f,
    endY: Float = Float.POSITIVE_INFINITY,
    base: Float = 10.0f,
    tileMode: TileMode = TileMode.Clamp,
): Brush = logarithmicGradient(colors, Offset(0.0f, startY), Offset(0.0f, endY), base, tileMode)

@Stable
fun verticalLogarithmicGradient(
    vararg colorStops: Pair<Float, Color>,
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
    base: Float = 10.0f,
    tileMode: TileMode = TileMode.Clamp,
): Brush =
    logarithmicGradient(
        *colorStops,
        start = Offset(0.0f, startY),
        end = Offset(0.0f, endY),
        base = base,
        tileMode = tileMode,
    )

@Immutable
class LogarithmicGradient
internal constructor(
    @Suppress("PrimitiveInCollection") internal val colors: List<Color>,
    @Suppress("PrimitiveInCollection") internal val stops: List<Float>? = null,
    internal val start: Offset,
    internal val end: Offset,
    internal val base: Float = 10.0f,
    internal val tileMode: TileMode = TileMode.Clamp,
) : ShaderBrush(), Interpolatable {

    override val intrinsicSize: Size
        get() =
            Size(
                if (start.x.isFinite() && end.x.isFinite()) abs(start.x - end.x) else Float.NaN,
                if (start.y.isFinite() && end.y.isFinite()) abs(start.y - end.y) else Float.NaN,
            )

    private fun logMap(t: Float, base: Float): Float {
        val clamped = when {
            t <= 0f -> 0f
            t >= 1f -> 1f
            else -> t
        }
        // base <= 0 or base == 1 are degenerate -> linear fallback
        if (base <= 0f || kotlin.math.abs(base - 1f) < 1e-6f) return clamped
        // f(t) = ln(1 + (base-1)*t) / ln(base)
        val numerator = ln(1f + (base - 1f) * clamped)
        val denom = ln(base)
        return (numerator / denom).coerceIn(0f, 1f)
    }

    override fun createShader(size: Size): Shader {
        val startX = if (start.x == Float.POSITIVE_INFINITY) size.width else start.x
        val startY = if (start.y == Float.POSITIVE_INFINITY) size.height else start.y
        val endX = if (end.x == Float.POSITIVE_INFINITY) size.width else end.x
        val endY = if (end.y == Float.POSITIVE_INFINITY) size.height else end.y

        val resolvedStops: List<Float> = when {
            stops != null -> {
                // transform provided stops using log mapping.
                stops.map { s ->
                    // ensure input stop is clamped to [0,1]
                    val clamped = s.coerceIn(0f, 1f)
                    logMap(clamped, base)
                }
            }
            else -> {
                // evenly spaced stops then transform
                if (colors.size <= 1) {
                    listOf(0f)
                } else {
                    val n = colors.size
                    List(n) { i ->
                        val linear = i.toFloat() / (n - 1).toFloat()
                        logMap(linear, base)
                    }
                }
            }
        }

        // LinearGradientShader expects colorStops in [0,1] increasing. Ensure monotonic increasing.
        val monotonicStops = run {
            val out = resolvedStops.toMutableList()
            // enforce non-decreasing by nudging tiny deltas
            for (i in 1 until out.size) {
                if (out[i] <= out[i - 1]) {
                    out[i] = min(1f, out[i - 1] + 1e-6f)
                }
            }
            out
        }

        return LinearGradientShader(
            colors = colors,
            colorStops = monotonicStops,
            from = Offset(startX, startY),
            to = Offset(endX, endY),
            tileMode = tileMode,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogarithmicGradient) return false

        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (start != other.start) return false
        if (end != other.end) return false
        if (base != other.base) return false
        if (tileMode != other.tileMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + base.hashCode()
        result = 31 * result + tileMode.hashCode()
        return result
    }

    override fun toString(): String {
        val startValue = if (start.isFinite) "start=$start, " else ""
        val endValue = if (end.isFinite) "end=$end, " else ""
        return "LogarithmicGradient(colors=$colors, " +
                "stops=$stops, " +
                startValue +
                endValue +
                "base=$base, tileMode=$tileMode)"
    }

    override fun lerp(other: Any?, t: Float): Any? {
        var otherLocal: Any? = other
        if (otherLocal == null) {
            otherLocal = SolidColor(Color.Transparent)
        }
        if (otherLocal is SolidColor) {
            otherLocal =
                LogarithmicGradient(
                    colors = colors.fastMap { otherLocal.value },
                    stops = stops,
                    start = start,
                    end = end,
                    base = base,
                    tileMode = tileMode,
                )
        }
        if (otherLocal is LogarithmicGradient) {
            return LogarithmicGradient(
                colors = lerpColorList(colors, otherLocal.colors, t),
                stops = lerpNullableFloatList(stops, otherLocal.stops, t),
                start = lerpSafe(start, otherLocal.start, t),
                end = lerpSafe(end, otherLocal.end, t),
                base = lerp(base, otherLocal.base, t),
                tileMode = if (t < 0.5f) tileMode else otherLocal.tileMode,
            )
        }
        return null
    }
}

private fun lerpColorList(left: List<Color>, right: List<Color>, t: Float): List<Color> {
    return List(maxOf(left.size, right.size)) {
        val l = minOf(it, left.size - 1)
        val r = minOf(it, right.size - 1)
        androidx.compose.ui.graphics.lerp(left[l], right[r], t)
    }
}

private fun lerpNullableFloatList(
    left: List<Float>?,
    right: List<Float>?,
    t: Float,
): List<Float>? {
    if (right == null || left == null) return null
    return lerpFloatList(left, right, t)
}

private fun lerpFloatList(left: List<Float>, right: List<Float>, t: Float): List<Float> {
    return List(maxOf(left.size, right.size)) {
        val l = minOf(it, left.size - 1)
        val r = minOf(it, right.size - 1)
        lerp(left[l], right[r], t)
    }
}

private fun lerpSafe(left: Offset, right: Offset, t: Float): Offset {
    return if (left.isFinite && right.isFinite) androidx.compose.ui.geometry.lerp(left, right, t)
    else if (t < 0.5f) left else right
}