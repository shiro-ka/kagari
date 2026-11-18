package dev.dimension.flare.ui.presenter.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import dev.dimension.flare.common.PagingState
import dev.dimension.flare.common.toPagingState
import dev.dimension.flare.data.datasource.microblog.KagariProfileListKind
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiList
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.route.KagariFeedRoute
import dev.dimension.flare.ui.route.KagariListRoute
import dev.dimension.flare.ui.route.KagariStarterPackRoute
import dev.dimension.flare.ui.route.toUri
import kotlinx.coroutines.flow.Flow

public class KagariProfileListPresenter(
    public val kind: KagariProfileListKind,
    public val accountKey: MicroBlogKey,
    private val source: Flow<PagingData<UiList>>,
) : PresenterBase<PagingState<UiList>>() {
    @Composable
    override fun body(): PagingState<UiList> {
        val scope = rememberCoroutineScope()
        return remember(source) { source.cachedIn(scope) }
            .collectAsLazyPagingItems()
            .toPagingState()
    }
}

public fun KagariProfileListPresenter.kagariItemUrl(item: UiList): String =
    when (kind) {
        KagariProfileListKind.Feeds -> KagariFeedRoute(accountKey = accountKey, uri = item.id)
        KagariProfileListKind.Lists -> KagariListRoute(accountKey = accountKey, uri = item.id, title = item.title)
        KagariProfileListKind.StarterPacks -> KagariStarterPackRoute(accountKey = accountKey, uri = item.id)
    }.toUri()
