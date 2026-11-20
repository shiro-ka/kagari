package dev.dimension.flare.data.datasource.microblog

import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.KagariMadoProfile

public interface KagariMadoSource {
    public suspend fun kagariMadoHasBoxes(userKey: MicroBlogKey): Boolean

    public suspend fun kagariMadoProfile(userKey: MicroBlogKey): KagariMadoProfile
}
