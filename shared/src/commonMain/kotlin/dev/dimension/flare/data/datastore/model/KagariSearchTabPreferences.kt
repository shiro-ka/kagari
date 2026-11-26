package dev.dimension.flare.data.datastore.model

import dev.dimension.flare.data.datasource.microblog.KagariSearchKind
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.serialization.Serializable

@Serializable
public data class KagariSearchTab(
    val accountKey: MicroBlogKey,
    val kind: KagariSearchKind,
    val mixed: Boolean? = null,
) {
    public fun isSameSource(other: KagariSearchTab): Boolean = accountKey == other.accountKey && kind == other.kind
}

@Serializable
internal data class KagariSearchTabPreferences(
    val initialized: Boolean = false,
    val tabs: List<KagariSearchTab> = emptyList(),
    val accountsTracked: Boolean = false,
    val knownAccounts: List<MicroBlogKey> = emptyList(),
    val mixedTabEnabled: Boolean = true,
)
