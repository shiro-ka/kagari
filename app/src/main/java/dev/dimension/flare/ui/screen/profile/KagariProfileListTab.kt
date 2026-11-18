package dev.dimension.flare.ui.screen.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.dimension.flare.ui.component.uiListItemComponent
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariProfileListPresenter
import dev.dimension.flare.ui.presenter.profile.kagariItemUrl
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariProfileListTab(
    id: String,
    presenter: KagariProfileListPresenter,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    val uriHandler = LocalUriHandler.current
    val items by producePresenter(key = "KagariProfileList_$id") {
        presenter.invoke()
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
