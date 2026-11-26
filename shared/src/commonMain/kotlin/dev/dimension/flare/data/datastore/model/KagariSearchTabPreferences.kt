package dev.dimension.flare.data.datastore.model

import dev.dimension.flare.data.datasource.microblog.KagariSearchKind
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.serialization.Serializable

@Serializable
public data class KagariSearchTab(
    val accountKey: MicroBlogKey,
    val kind: KagariSearchKind,
)

@Serializable
internal data class KagariSearchTabPreferences(
    val initialized: Boolean = false,
    val tabs: List<KagariSearchTab> = emptyList(),
    val accountsTracked: Boolean = false,
    val knownAccounts: List<MicroBlogKey> = emptyList(),
)
