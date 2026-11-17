package dev.dimension.flare.data.datasource.microblog

import dev.dimension.flare.ui.model.UiProfile

public class KagariMentionSearch(
    public val search: suspend (String) -> List<UiProfile>,
)
