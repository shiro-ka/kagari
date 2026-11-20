package dev.dimension.flare.ui.screen.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Xmark
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.microblog.ComposeData
import dev.dimension.flare.data.datasource.microblog.KagariComposeLinks
import dev.dimension.flare.data.datasource.microblog.KagariLinkCardPreview
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.NetworkImage
import dev.dimension.flare.ui.presenter.compose.ComposeStatus
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val KAGARI_LINK_CARD_DELAY_MILLIS = 700L

@Stable
internal class KagariLinkCardState {
    var dismissedUrl by mutableStateOf<String?>(null)

    fun isDismissed(url: String?): Boolean = url != null && url == dismissedUrl

    fun restore(
        links: KagariComposeLinks?,
        data: ComposeData,
    ) {
        dismissedUrl =
            if (data.kagariNoLinkCard) {
                links?.firstLink(data.content)
            } else {
                null
            }
    }
}

@Composable
internal fun rememberKagariLinkCardState(): KagariLinkCardState = remember { KagariLinkCardState() }

internal fun KagariComposeLinks?.kagariCardUrl(
    text: CharSequence,
    hasMedia: Boolean,
    status: ComposeStatus?,
): String? =
    if (this == null || hasMedia || status is ComposeStatus.Quote) {
        null
    } else {
        firstLink(text.toString())
    }

@Composable
internal fun rememberKagariLinkHighlight(links: KagariComposeLinks?): OutputTransformation? {
    val color = MaterialTheme.colorScheme.primary
    return remember(links, color) {
        links?.let { kagariLinkHighlight(it, color) }
    }
}

private fun kagariLinkHighlight(
    links: KagariComposeLinks,
    color: Color,
): OutputTransformation {
    val style = SpanStyle(color = color)
    return OutputTransformation {
        val text = asCharSequence().toString()
        links.highlights(text).forEach { range ->
            val end = range.last + 1
            if (range.first in 0..<end && end <= length) {
                addStyle(style, range.first, end)
            }
        }
    }
}

@Composable
internal fun KagariComposeLinkCard(
    links: KagariComposeLinks?,
    url: String?,
    dismissed: Boolean,
    onDismiss: () -> Unit,
) {
    if (links == null) return
    var preview by remember { mutableStateOf<KagariLinkCardPreview?>(null) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(url) {
        preview = null
        loading = false
        val target = url ?: return@LaunchedEffect
        delay(KAGARI_LINK_CARD_DELAY_MILLIS)
        loading = true
        preview =
            try {
                links.linkCard(target)
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Exception) {
                null
            }
        loading = false
    }
    if (url == null || dismissed) return
    val card = preview
    if (card == null) {
        if (loading) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenHorizontalPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        return
    }
    OutlinedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = screenHorizontalPadding, vertical = 8.dp),
    ) {
        Box {
            Column {
                card.image?.let { image ->
                    NetworkImage(
                        model = image,
                        contentDescription = card.title,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.91f),
                    )
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (card.domain.isNotEmpty()) {
                        Text(
                            text = card.domain,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.8f),
                        contentColor = Color.White,
                    ),
            ) {
                FAIcon(
                    imageVector = FontAwesomeIcons.Solid.Xmark,
                    contentDescription = stringResource(id = R.string.kagari_compose_remove_link_card),
                )
            }
        }
    }
}
