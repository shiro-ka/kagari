package dev.dimension.flare.ui.screen.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Xmark
import dev.dimension.flare.R
import dev.dimension.flare.ui.component.FAIcon
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class KagariCropShape(
    val aspect: Float,
    val outWidth: Int,
) {
    Avatar(1f, 1000),
    Banner(3f, 1500),
}

internal fun kagariLoadBitmap(
    context: Context,
    uri: Uri,
    maxSide: Int = 2048,
): Bitmap? =
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

internal fun kagariEncodeUnder(
    bitmap: Bitmap,
    limit: Int = 950_000,
): ByteArray {
    var quality = 92
    while (true) {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val bytes = out.toByteArray()
        if (bytes.size <= limit || quality <= 40) return bytes
        quality -= 8
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariImageCropper(
    bitmap: Bitmap,
    shape: KagariCropShape,
    onCancel: () -> Unit,
    onCropped: (Bitmap) -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var canvasSize by remember { mutableStateOf(Size.Zero) }
        val image = remember(bitmap) { bitmap.asImageBitmap() }

        fun frame(size: Size): Rect {
            val margin = 24f
            val width = size.width - margin * 2
            val height = width / shape.aspect
            val left = margin
            val top = (size.height - height) / 2f
            return Rect(left, top, left + width, top + height)
        }

        fun baseScale(frame: Rect): Float = max(frame.width / bitmap.width, frame.height / bitmap.height)

        fun clampOffset(
            value: Offset,
            size: Size,
            zoom: Float,
        ): Offset {
            val f = frame(size)
            val s = baseScale(f) * zoom
            val maxX = max(0f, (bitmap.width * s - f.width) / 2f)
            val maxY = max(0f, (bitmap.height * s - f.height) / 2f)
            return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
        }

        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            TopAppBar(
                title = {
                    Text(
                        text =
                            stringResource(
                                if (shape == KagariCropShape.Avatar) R.string.kagari_crop_avatar else R.string.kagari_crop_banner,
                            ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        FAIcon(imageVector = FontAwesomeIcons.Solid.Xmark, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val f = frame(canvasSize)
                            val s = baseScale(f) * scale
                            val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f) + offset
                            val imageLeft = center.x - bitmap.width * s / 2f
                            val imageTop = center.y - bitmap.height * s / 2f
                            val x = ((f.left - imageLeft) / s).roundToInt().coerceIn(0, bitmap.width - 1)
                            val y = ((f.top - imageTop) / s).roundToInt().coerceIn(0, bitmap.height - 1)
                            val w = (f.width / s).roundToInt().coerceIn(1, bitmap.width - x)
                            val h = (f.height / s).roundToInt().coerceIn(1, bitmap.height - y)
                            val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                            val outWidth = min(shape.outWidth, w)
                            val outHeight = (outWidth / shape.aspect).roundToInt().coerceAtLeast(1)
                            onCropped(Bitmap.createScaledBitmap(cropped, outWidth, outHeight, true))
                        },
                    ) {
                        Text(text = stringResource(R.string.kagari_crop_done))
                    }
                },
            )
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .pointerInput(bitmap) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val size = Size(this.size.width.toFloat(), this.size.height.toFloat())
                                    scale = (scale * zoom).coerceIn(1f, 6f)
                                    offset = clampOffset(offset + pan, size, scale)
                                }
                            },
                ) {
                    canvasSize = size
                    val f = frame(size)
                    val s = baseScale(f) * scale
                    val drawWidth = bitmap.width * s
                    val drawHeight = bitmap.height * s
                    val center = Offset(size.width / 2f, size.height / 2f) + offset
                    drawImage(
                        image = image,
                        dstOffset = IntOffset((center.x - drawWidth / 2f).roundToInt(), (center.y - drawHeight / 2f).roundToInt()),
                        dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt()),
                    )
                    val mask =
                        Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(Rect(Offset.Zero, size))
                            if (shape == KagariCropShape.Avatar) addOval(f) else addRect(f)
                        }
                    drawPath(mask, Color.Black.copy(alpha = 0.55f))
                    if (shape == KagariCropShape.Avatar) {
                        drawOval(Color.White, topLeft = f.topLeft, size = f.size, style = Stroke(width = 2.dp.toPx()))
                    } else {
                        drawRect(Color.White, topLeft = f.topLeft, size = f.size, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
        }
    }
}
