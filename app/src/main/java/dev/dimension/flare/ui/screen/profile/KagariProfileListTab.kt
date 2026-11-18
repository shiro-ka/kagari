package dev.dimension.flare.ui.screen.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.common.PagingState
import dev.dimension.flare.data.datasource.microblog.KagariProfileListKind
import dev.dimension.flare.ui.component.uiListItemComponent
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariProfileListPresenter
import dev.dimension.flare.ui.presenter.profile.kagariItemUrl
import dev.dimension.flare.ui.screen.KagariMessageBox
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariProfileListTab(
    id: String,
    presenter: KagariProfileListPresenter,
    bottomPadding: Dp,
) {
    val uriHandler = LocalUriHandler.current
    val items by producePresenter(key = "KagariProfileList_$id") {
        presenter.invoke()
    }
    if (items is PagingState.Empty) {
        KagariMessageBox(
            message =
                stringResource(
                    when (presenter.kind) {
                        KagariProfileListKind.Feeds -> R.string.kagari_profile_list_empty_feeds
                        KagariProfileListKind.StarterPacks -> R.string.kagari_profile_list_empty_starter_packs
                        KagariProfileListKind.Lists -> R.string.kagari_profile_list_empty_lists
                    },
                ),
            padding = PaddingValues(bottom = bottomPadding),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + bottomPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        uiListItemComponent(
            items = items,
            onClicked = { item -> uriHandler.openUri(presenter.kagariItemUrl(item)) },
        )
    }
}
