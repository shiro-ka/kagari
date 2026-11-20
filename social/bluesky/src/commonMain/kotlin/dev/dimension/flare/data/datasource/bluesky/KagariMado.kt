package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.GetProfileQueryParams
import dev.dimension.flare.data.network.bluesky.KagariMadoClient
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.KagariMadoProfile
import sh.christian.ozone.api.Did

internal suspend fun BlueskyDataSource.kagariLoadMadoHasBoxes(userKey: MicroBlogKey): Boolean = KagariMadoClient.hasBoxes(userKey.id)

internal suspend fun BlueskyDataSource.kagariLoadMadoProfile(userKey: MicroBlogKey): KagariMadoProfile {
    val handle =
        kagariService()
            .getProfile(GetProfileQueryParams(actor = Did(did = userKey.id)))
            .requireResponse()
            .handle
            .handle
    return KagariMadoClient.profile(userKey.id, handle)
}
