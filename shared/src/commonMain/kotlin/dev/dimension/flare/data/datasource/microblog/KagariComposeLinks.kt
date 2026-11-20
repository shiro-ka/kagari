package dev.dimension.flare.data.datasource.microblog

import androidx.compose.runtime.Immutable

@Immutable
public data class KagariLinkCardPreview(
    val uri: String,
    val title: String,
    val domain: String,
    val imageUrl: String?,
)

public class KagariComposeLinks(
    public val highlights: (String) -> List<IntRange>,
    public val firstLink: (String) -> String?,
    public val linkCard: suspend (String) -> KagariLinkCardPreview?,
)
