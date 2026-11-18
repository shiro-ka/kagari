package dev.dimension.flare.ui.screen.dm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.dimension.flare.R
import dev.dimension.flare.common.PagingState
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.BackButton
import dev.dimension.flare.ui.component.dm.dmList
import dev.dimension.flare.ui.model.UiDMRoom
import dev.dimension.flare.ui.presenter.dm.KagariDMRequestsPresenter
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import moe.tlaster.precompose.molecule.producePresenter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariDMRequestsScreen(
    accountType: AccountType,
    onBack: () -> Unit,
    onItemClicked: (MicroBlogKey) -> Unit,
) {
    val items by producePresenter("kagari_dm_requests_$accountType") {
        remember(accountType) { KagariDMRequestsPresenter(accountType) }.invoke()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.kagari_dm_requests)) },
                navigationIcon = { BackButton(onBack = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            dmList(data = items, onItemClicked = onItemClicked)
        }
    }
}

internal fun LazyListScope.kagariDMRequestsRow(
    requests: PagingState<UiDMRoom>,
    onClick: () -> Unit,
) {
    val count = (requests as? PagingState.Success)?.itemCount ?: 0
    if (count > 0) {
        item(key = "kagari_dm_requests") {
            KagariDMRequestsRow(count = count, onClick = onClick)
        }
    }
}

@Composable
private fun KagariDMRequestsRow(
    count: Int,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = { Text(text = "$count ›") },
        content = {
            Text(
                text = stringResource(R.string.kagari_dm_requests),
                color = MaterialTheme.colorScheme.primary,
            )
        },
    )
}
