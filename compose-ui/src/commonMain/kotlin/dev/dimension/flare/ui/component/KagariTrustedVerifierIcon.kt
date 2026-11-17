package dev.dimension.flare.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val KagariTrustedVerifierIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "KagariTrustedVerifier",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                val bumps = 12
                val inner = 9.6f
                val outer = 12.6f
                val step = 2 * PI / bumps
                moveTo(12f + inner, 12f)
                for (i in 0 until bumps) {
                    val mid = step * (i + 0.5)
                    val end = step * (i + 1)
                    quadTo(
                        12f + outer * cos(mid).toFloat(),
                        12f + outer * sin(mid).toFloat(),
                        12f + inner * cos(end).toFloat(),
                        12f + inner * sin(end).toFloat(),
                    )
                }
                close()
                moveTo(6.0f, 12.6f)
                lineTo(7.6f, 11.0f)
                lineTo(10.4f, 13.8f)
                lineTo(16.4f, 7.8f)
                lineTo(18.0f, 9.4f)
                lineTo(10.4f, 17.0f)
                close()
            }
        }.build()
}
