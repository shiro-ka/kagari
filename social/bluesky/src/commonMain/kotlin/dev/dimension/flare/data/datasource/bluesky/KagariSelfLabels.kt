package dev.dimension.flare.data.datasource.bluesky

import app.bsky.feed.PostLabelsUnion
import com.atproto.label.SelfLabel
import com.atproto.label.SelfLabels
import kotlinx.collections.immutable.toPersistentList

internal val kagariAdultSelfLabels = listOf("sexual", "nudity", "porn")
internal val kagariOtherSelfLabels = listOf("graphic-media")

internal fun kagariSelfLabels(values: List<String>): PostLabelsUnion? {
    val allowed = values.filter { it in kagariAdultSelfLabels || it in kagariOtherSelfLabels }.distinct()
    if (allowed.isEmpty()) return null
    return PostLabelsUnion.SelfLabels(
        SelfLabels(
            values = allowed.map { SelfLabel(`val` = it) }.toPersistentList(),
        ),
    )
}
