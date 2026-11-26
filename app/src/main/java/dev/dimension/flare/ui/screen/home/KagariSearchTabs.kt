package dev.dimension.flare.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Bars
import compose.icons.fontawesomeicons.solid.MagnifyingGlass
import compose.icons.fontawesomeicons.solid.Pen
import compose.icons.fontawesomeicons.solid.Trash
import dev.dimension.flare.R
import dev.dimension.flare.common.isEmpty
import dev.dimension.flare.common.onLoading
import dev.dimension.flare.common.onSuccess
import dev.dimension.flare.data.datasource.microblog.KagariSearchKind
import dev.dimension.flare.data.datastore.model.KagariSearchTab
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.AvatarComponent
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.TabRowIndicator
import dev.dimension.flare.ui.component.status.AdaptiveCard
import dev.dimension.flare.ui.component.status.CommonStatusHeaderComponent
import dev.dimension.flare.ui.component.status.ListEmptyView
import dev.dimension.flare.ui.component.status.UserPlaceholder
import dev.dimension.flare.ui.component.status.pagingError
import dev.dimension.flare.ui.component.status.status
import dev.dimension.flare.ui.presenter.home.KagariSearchTabsState
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import dev.dimension.flare.ui.theme.segmentedShapes2
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun KagariSearchKind.label(): String =
    stringResource(
        when (this) {
            KagariSearchKind.Posts -> R.string.kagari_search_kind_posts
            KagariSearchKind.Top -> R.string.kagari_search_kind_top
            KagariSearchKind.Latest -> R.string.kagari_search_kind_latest
            KagariSearchKind.Users -> R.string.kagari_search_kind_users
            KagariSearchKind.Media -> R.string.kagari_search_kind_media
            KagariSearchKind.Oldest -> R.string.kagari_search_kind_oldest
            KagariSearchKind.Popular -> R.string.kagari_search_kind_popular
        },
    )

private fun KagariSearchTab.itemKey(): String = "${accountKey}_${kind.name}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariSearchTabRow(
    state: KagariSearchTabsState,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.tabs.isNotEmpty()) {
            SecondaryScrollableTabRow(
                selectedTabIndex = state.selectedIndex,
                containerColor = Color.Transparent,
                indicator = {
                    TabRowIndicator(
                        selectedIndex = state.selectedIndex,
                    )
                },
                divider = {},
                modifier = Modifier.weight(1f),
            ) {
                state.tabs.forEachIndexed { index, item ->
                    LeadingIconTab(
                        modifier = Modifier.clip(CircleShape),
                        selected = index == state.selectedIndex,
                        onClick = {
                            state.select(index)
                        },
                        text = {
                            Text(
                                text = item.tab.kind.label(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = {
                            AvatarComponent(
                                item.profile.avatar,
                                size = 24.dp,
                                contentDescription = item.profile.handle.display,
                            )
                        },
                    )
                }
            }
        } else {
            Row(modifier = Modifier.weight(1f)) {}
        }
        IconButton(onClick = onEdit) {
            FAIcon(
                FontAwesomeIcons.Solid.Pen,
                contentDescription = stringResource(R.string.kagari_search_tabs_edit),
            )
        }
    }
}

internal fun LazyStaggeredGridScope.kagariSearchTabContent(state: KagariSearchTabsState) {
    if (state.searching && (state.tabs.isEmpty() || state.hasNoResults)) {
        item(span = StaggeredGridItemSpan.FullLine) {
            ListEmptyView(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                text =
                    stringResource(
                        if (state.tabs.isEmpty()) R.string.search_unavailable else R.string.empty_search_results,
                    ),
                icon = FontAwesomeIcons.Solid.MagnifyingGlass,
            )
        }
    }
    state.users
        .onSuccess {
            items(count = itemCount) { index ->
                val item = get(index)
                AdaptiveCard {
                    if (item == null) {
                        UserPlaceholder(
                            modifier = Modifier.padding(8.dp),
                        )
                    } else {
                        CommonStatusHeaderComponent(
                            modifier = Modifier.padding(8.dp),
                            data = item,
                        )
                    }
                }
            }
        }.onLoading {
            items(count = 10) {
                AdaptiveCard {
                    UserPlaceholder(
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    pagingError(state.users)
    if (!state.status.isEmpty) {
        status(state.status)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun KagariSearchTabsSheet(
    state: KagariSearchTabsState,
    onDismissRequest: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val tabs =
        remember {
            mutableStateListOf<KagariSearchTab>().apply {
                addAll(state.tabs.map { it.tab })
            }
        }
    val profiles = state.accountOptions.associate { it.profile.key to it.profile }
    ModalBottomSheet(
        onDismissRequest = {
            state.saveTabs(tabs.toList())
            onDismissRequest()
        },
    ) {
        val lazyListState = rememberLazyListState()
        val reorderableState =
            rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromIndex = tabs.indexOfFirst { it.itemKey() == from.key }
                val toIndex = tabs.indexOfFirst { it.itemKey() == to.key }
                if (fromIndex != -1 && toIndex != -1) {
                    tabs.add(toIndex, tabs.removeAt(fromIndex))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = screenHorizontalPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.kagari_search_tabs_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (tabs.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.kagari_search_tabs_empty),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            itemsIndexed(tabs, key = { _, tab -> tab.itemKey() }) { index, tab ->
                ReorderableItem(reorderableState, key = tab.itemKey()) { isDragging ->
                    val profile = profiles[tab.accountKey]
                    SegmentedListItem(
                        selected = isDragging,
                        onClick = {},
                        shapes = ListItemDefaults.segmentedShapes2(index, tabs.size),
                        content = {
                            Text(tab.kind.label())
                        },
                        supportingContent = {
                            profile?.let {
                                Text(
                                    text = it.handle.display,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        leadingContent = {
                            AvatarComponent(
                                profile?.avatar,
                                size = 24.dp,
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = {
                                        tabs.remove(tab)
                                    },
                                ) {
                                    FAIcon(
                                        FontAwesomeIcons.Solid.Trash,
                                        contentDescription = stringResource(R.string.tab_settings_remove),
                                    )
                                }
                                IconButton(
                                    modifier =
                                        Modifier.draggableHandle(
                                            onDragStarted = {
                                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                            },
                                            onDragStopped = {
                                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                            },
                                        ),
                                    onClick = {},
                                ) {
                                    FAIcon(
                                        FontAwesomeIcons.Solid.Bars,
                                        contentDescription = stringResource(R.string.tab_settings_drag),
                                    )
                                }
                            }
                        },
                    )
                }
            }
            item(key = "add") {
                KagariSearchTabAddSection(
                    state = state,
                    tabs = tabs,
                    onAdd = { tabs.add(it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KagariSearchTabAddSection(
    state: KagariSearchTabsState,
    tabs: List<KagariSearchTab>,
    onAdd: (KagariSearchTab) -> Unit,
) {
    var selectedAccount by remember { mutableStateOf<MicroBlogKey?>(state.accountOptions.firstOrNull()?.profile?.key) }
    var selectedKind by remember { mutableStateOf<KagariSearchKind?>(null) }
    val option = state.accountOptions.firstOrNull { it.profile.key == selectedAccount }
    val candidate = option?.let { account -> selectedKind?.let { KagariSearchTab(account.profile.key, it) } }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.kagari_search_tabs_add),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.kagari_search_tabs_account),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.accountOptions.forEach { account ->
                FilterChip(
                    selected = account.profile.key == selectedAccount,
                    onClick = {
                        selectedAccount = account.profile.key
                        selectedKind = null
                    },
                    label = {
                        Text(
                            text = account.profile.handle.display,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        AvatarComponent(
                            account.profile.avatar,
                            size = 18.dp,
                        )
                    },
                )
            }
        }
        if (option != null) {
            Text(
                text = stringResource(R.string.kagari_search_tabs_kind),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                option.kinds.forEach { kind ->
                    FilterChip(
                        selected = kind == selectedKind,
                        onClick = {
                            selectedKind = kind
                        },
                        label = {
                            Text(kind.label())
                        },
                    )
                }
            }
        }
        Button(
            enabled = candidate != null && candidate !in tabs,
            onClick = {
                candidate?.let(onAdd)
                selectedKind = null
            },
        ) {
            Text(stringResource(R.string.kagari_search_tabs_add_confirm))
        }
    }
}
