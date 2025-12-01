package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

//fun RoundedPolygon.toPath(path: Path = Path()): Path {
//    pathFromCubics(path, cubics)
//    return path
//}
//
//fun Morph.toPath(progress: Float, path: Path = Path()): Path {
//    pathFromCubics(path, asCubics(progress))
//    return path
//}
//
//fun pathFromCubics(path: Path, cubics: List<Cubic>) {
//    var first = true
//    path.rewind()
//    for (i in 0 until cubics.size) {
//        val cubic = cubics[i]
//        if (first) {
//            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
//            first = false
//        }
//        path.cubicTo(
//            cubic.control0X,
//            cubic.control0Y,
//            cubic.control1X,
//            cubic.control1Y,
//            cubic.anchor1X,
//            cubic.anchor1Y
//        )
//    }
//    path.close()
//}
//
//// TODO fix
//private class ShapeFromPath(private val path: Path) : Shape {
//    private val matrix = Matrix()
//
//    override fun createOutline(
//        size: Size,
//        layoutDirection: LayoutDirection,
//        density: Density
//    ): Outline {
//        // Below assumes that you haven't changed the default radius of 1f, nor the centerX and centerY of 0f
//        // By default this stretches the path to the size of the container, if you don't want stretching, use the same size.width for both x and y.
//        matrix.scale(size.width / 2f, size.height / 2f)
//        matrix.translate(1f, 1f)
//
//        path.transform(matrix)
//        return Outline.Generic(path)
//    }
//}
//
//fun Path.toShape(): Shape = ShapeFromPath(this)
//
//fun RoundedPolygon.toShape(): Shape = ShapeFromPath(toPath())
//
//class RotatingShape(
//    private val path: Path,
//    private val rotation: Float
//) : Shape {
//    private val matrix = Matrix()
//
//    override fun createOutline(
//        size: Size,
//        layoutDirection: LayoutDirection,
//        density: Density
//    ): Outline {
//        // Below assumes that you haven't changed the default radius of 1f, nor the centerX and centerY of 0f
//        // By default this stretches the path to the size of the container, if you don't want stretching, use the same size.width for both x and y.
//        matrix.scale(size.width / 2f, size.height / 2f)
//        matrix.translate(1f, 1f)
//        matrix.rotateZ(rotation)
//
//        path.transform(matrix)
//        return Outline.Generic(path)
//    }
//}
//
//class MorphingShape(
//    private val morph: Morph,
//    private val progress: Float
//) : Shape {
//    private val matrix = Matrix()
//
//    override fun createOutline(
//        size: Size,
//        layoutDirection: LayoutDirection,
//        density: Density
//    ): Outline {
//        // Below assumes that you haven't changed the default radius of 1f, nor the centerX and centerY of 0f
//        // By default this stretches the path to the size of the container, if you don't want stretching, use the same size.width for both x and y.
//        matrix.scale(size.width / 2f, size.height / 2f)
//        matrix.translate(1f, 1f)
//
//        val path = morph.toPath(progress = progress)
//        path.transform(matrix)
//        return Outline.Generic(path)
//    }
//}
//
class RotatingMorphingShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotation: Float
) : Shape {

    private val matrix = Matrix()
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Below assumes that you haven't changed the default radius of 1f, nor the centerX and centerY of 0f
        // By default this stretches the path to the size of the container, if you don't want stretching, use the same size.width for both x and y.
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        matrix.rotateZ(rotation)

        val path = morph.toPath(progress = progress)
        path.transform(matrix)

        return Outline.Generic(path)
    }
}