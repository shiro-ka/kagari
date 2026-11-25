package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import dev.dimension.flare.ui.component.NetworkImage
import dev.dimension.flare.ui.model.UiMedia
import dev.dimension.flare.ui.theme.PlatformTheme

internal const val KAGARI_LINK_CARD_ASPECT_RATIO: Float = 1.91f

@Composable
public fun KagariLinkCardImage(
    image: UiMedia.Image,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var loadedPortrait by remember(image.previewUrl) { mutableStateOf(false) }
    val portrait =
        if (image.width > 0f && image.height > 0f) {
            image.height > image.width
        } else {
            loadedPortrait
        }
    val letterbox = PlatformTheme.colorScheme.cardAlt
    NetworkImage(
        model = image.preview,
        contentDescription = contentDescription,
        contentScale = if (portrait) ContentScale.Fit else ContentScale.Crop,
        onIntrinsicSize = { size ->
            loadedPortrait = size.height > size.width
        },
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(KAGARI_LINK_CARD_ASPECT_RATIO)
                .clipToBounds()
                .let {
                    if (portrait) {
                        it.background(letterbox)
                    } else {
                        it
                    }
                },
    )
}
