package dev.dimension.flare.data.datasource.microblog

import androidx.compose.runtime.Immutable
import dev.dimension.flare.ui.model.UiMedia

@Immutable
public data class KagariLinkCardPreview(
    val uri: String,
    val title: String,
    val domain: String,
    val image: UiMedia.Image?,
)

public class KagariComposeLinks(
    public val highlights: (String) -> List<IntRange>,
    public val firstLink: (String) -> String?,
    public val linkCard: suspend (String) -> KagariLinkCardPreview?,
)
