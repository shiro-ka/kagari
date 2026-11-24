package dev.dimension.flare.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Heart
import compose.icons.fontawesomeicons.solid.Heart
import compose.icons.fontawesomeicons.solid.Lock

internal val KagariPrivateBookmarkIcon: ImageVector by lazy {
    kagariLockedHeart("KagariPrivateBookmark", FontAwesomeIcons.Regular.Heart)
}

internal val KagariPrivateBookmarkedIcon: ImageVector by lazy {
    kagariLockedHeart("KagariPrivateBookmarked", FontAwesomeIcons.Solid.Heart)
}

private fun kagariLockedHeart(
    name: String,
    heart: ImageVector,
): ImageVector {
    val lock = FontAwesomeIcons.Solid.Lock
    val heartScale = 400f / heart.viewportWidth
    val lockScale = 240f / lock.viewportHeight
    return ImageVector
        .Builder(
            name = name,
            defaultWidth = 512.dp,
            defaultHeight = 512.dp,
            viewportWidth = 512f,
            viewportHeight = 512f,
        ).apply {
            addVector(
                source = heart,
                scale = heartScale,
                translationX = 0f,
                translationY = (512f - heart.viewportHeight * heartScale) / 2f,
            )
            addVector(
                source = lock,
                scale = lockScale,
                translationX = 512f - lock.viewportWidth * lockScale,
                translationY = 512f - lock.viewportHeight * lockScale,
            )
        }.build()
}

private fun ImageVector.Builder.addVector(
    source: ImageVector,
    scale: Float,
    translationX: Float,
    translationY: Float,
) {
    group(
        scaleX = scale,
        scaleY = scale,
        translationX = translationX,
        translationY = translationY,
    ) {
        addNodes(source.root)
    }
}

private fun ImageVector.Builder.addNodes(source: VectorGroup) {
    source.forEach { node ->
        when (node) {
            is VectorPath -> {
                addPath(
                    pathData = node.pathData,
                    pathFillType = node.pathFillType,
                    name = node.name,
                    fill = node.fill,
                    fillAlpha = node.fillAlpha,
                    stroke = node.stroke,
                    strokeAlpha = node.strokeAlpha,
                    strokeLineWidth = node.strokeLineWidth,
                    strokeLineCap = node.strokeLineCap,
                    strokeLineJoin = node.strokeLineJoin,
                    strokeLineMiter = node.strokeLineMiter,
                )
            }

            is VectorGroup -> {
                group(
                    name = node.name,
                    rotate = node.rotation,
                    pivotX = node.pivotX,
                    pivotY = node.pivotY,
                    scaleX = node.scaleX,
                    scaleY = node.scaleY,
                    translationX = node.translationX,
                    translationY = node.translationY,
                    clipPathData = node.clipPathData,
                ) {
                    addNodes(node)
                }
            }
        }
    }
}
