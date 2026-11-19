package dev.dimension.flare.ui.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.dimension.flare.data.database.cache.CacheDatabase
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiProfile
import kotlinx.coroutines.flow.map

public class KagariCachedProfilePresenter(
    private val userKey: MicroBlogKey,
) : PresenterBase<KagariCachedProfileState>() {
    private val database: CacheDatabase by koinInject()

    @Composable
    override fun body(): KagariCachedProfileState {
        val profile by remember(userKey) {
            database.userDao().findByKey(userKey).map { it?.content }
        }.collectAsState(null)
        return KagariCachedProfileState(profile)
    }
}

public data class KagariCachedProfileState(
    val profile: UiProfile?,
)

public fun KagariCachedProfileState.kagariAccountLabel(userKey: MicroBlogKey): String =
    profile?.handle?.display ?: "${userKey.id}@${userKey.host}"
