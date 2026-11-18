package dev.dimension.flare.data.datasource.microblog

import androidx.paging.PagingData
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiList
import dev.dimension.flare.ui.model.UiStrings
import dev.dimension.flare.ui.model.UiText
import dev.dimension.flare.ui.model.asText
import kotlinx.coroutines.flow.Flow

public enum class KagariProfileListKind {
    Feeds,
    StarterPacks,
    Lists,
}

public interface KagariProfileListSource {
    public suspend fun kagariProfileListKinds(userKey: MicroBlogKey): List<KagariProfileListKind>

    public fun kagariProfileList(
        userKey: MicroBlogKey,
        kind: KagariProfileListKind,
    ): Flow<PagingData<UiList>>
}

public val KagariProfileListKind.kagariTabName: UiText
    get() =
        when (this) {
            KagariProfileListKind.Feeds -> UiStrings.Feeds.asText()
            KagariProfileListKind.StarterPacks -> UiStrings.KagariStarterPacks.asText()
            KagariProfileListKind.Lists -> UiStrings.List.asText()
        }
