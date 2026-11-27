package dev.dimension.flare.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.MagnifyingGlass
import compose.icons.fontawesomeicons.solid.Pen
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.SquareRss
import dev.dimension.flare.R
import dev.dimension.flare.common.isEmpty
import dev.dimension.flare.common.onLoading
import dev.dimension.flare.common.onSuccess
import dev.dimension.flare.data.datasource.microblog.KagariSearchKind
import dev.dimension.flare.data.datasource.microblog.kagariCanMix
import dev.dimension.flare.data.datastore.model.KagariSearchTab
import dev.dimension.flare.ui.component.AvatarComponent
import dev.dimension.flare.ui.component.BackButton
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.FlareLargeFlexibleTopAppBar
import dev.dimension.flare.ui.component.FlareScaffold
import dev.dimension.flare.ui.component.TabRowIndicator
import dev.dimension.flare.ui.component.listCard
import dev.dimension.flare.ui.component.status.AdaptiveCard
import dev.dimension.flare.ui.component.status.CommonStatusHeaderComponent
import dev.dimension.flare.ui.component.status.ListEmptyView
import dev.dimension.flare.ui.component.status.UserPlaceholder
import dev.dimension.flare.ui.component.status.pagingError
import dev.dimension.flare.ui.component.status.status
import dev.dimension.flare.ui.presenter.home.KagariSearchAccountOption
import dev.dimension.flare.ui.presenter.home.KagariSearchTabItem
import dev.dimension.flare.ui.presenter.home.KagariSearchTabsEditPresenter
import dev.dimension.flare.ui.presenter.home.KagariSearchTabsState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.screen.settings.AccountLeadingIconTab
import dev.dimension.flare.ui.screen.settings.ListTabItem
import dev.dimension.flare.ui.screen.settings.SwipeToDeleteTabItem
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import dev.dimension.flare.ui.theme.segmentedShapes2
import dev.dimension.flare.ui.theme.single
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import moe.tlaster.precompose.molecule.producePresenter
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
                                text =
                                    when (item) {
                                        is KagariSearchTabItem.Single -> item.tab.kind.label()
                                        KagariSearchTabItem.Mixed -> stringResource(R.string.kagari_search_mixed_tab)
                                    },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = {
                            when (item) {
                                is KagariSearchTabItem.Single -> {
                                    AvatarComponent(
                                        item.profile.avatar,
                                        size = 24.dp,
                                        contentDescription = item.profile.handle.display,
                                    )
                                }

                                KagariSearchTabItem.Mixed -> {
                                    FAIcon(
                                        FontAwesomeIcons.Solid.SquareRss,
                                        contentDescription = null,
                                    )
                                }
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariSearchTabSettingScreen(onBack: () -> Unit) {
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val haptics = LocalHapticFeedback.current
    val state by producePresenter {
        remember { KagariSearchTabsEditPresenter() }.invoke()
    }
    val tabs = remember { mutableStateListOf<KagariSearchTab>() }
    var loaded by remember { mutableStateOf(false) }
    var showAddTab by remember { mutableStateOf(false) }
    var mixedTabEnabled by remember { mutableStateOf(true) }
    var editingTab by remember { mutableStateOf<KagariSearchTab?>(null) }
    LaunchedEffect(state.loaded) {
        if (state.loaded && !loaded) {
            tabs.addAll(state.tabs)
            mixedTabEnabled = state.mixedTabEnabled
            loaded = true
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (loaded) {
                state.save(tabs.toList(), mixedTabEnabled)
            }
        }
    }
    FlareScaffold(
        topBar = {
            FlareLargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(R.string.kagari_search_tabs_title))
                },
                navigationIcon = {
                    BackButton(onBack = onBack)
                },
                actions = {
                    IconButton(
                        onClick = {
                            showAddTab = true
                        },
                    ) {
                        FAIcon(
                            imageVector = FontAwesomeIcons.Solid.Plus,
                            contentDescription = stringResource(R.string.kagari_search_tabs_add),
                        )
                    }
                },
                scrollBehavior = topAppBarScrollBehavior,
            )
        },
        modifier =
            Modifier
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
    ) { contentPadding ->
        val lazyListState = rememberLazyListState()
        val reorderableLazyColumnState =
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
            contentPadding = contentPadding,
            modifier =
                Modifier
                    .padding(horizontal = screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            if (loaded && tabs.isEmpty()) {
                item {
                    ListEmptyView(
                        modifier = Modifier.fillParentMaxSize(),
                        text = stringResource(R.string.kagari_search_tabs_empty),
                        icon = FontAwesomeIcons.Solid.MagnifyingGlass,
                    )
                }
            }
            if (loaded) {
                item("header") {
                    SegmentedListItem(
                        onClick = {
                            mixedTabEnabled = !mixedTabEnabled
                        },
                        shapes = ListItemDefaults.single(),
                        content = {
                            Text(stringResource(R.string.kagari_search_mixed_tab_setting))
                        },
                        trailingContent = {
                            Switch(
                                checked = mixedTabEnabled,
                                onCheckedChange = {
                                    mixedTabEnabled = it
                                },
                            )
                        },
                        supportingContent = {
                            Text(stringResource(R.string.kagari_search_mixed_tab_setting_description))
                        },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            itemsIndexed(tabs, key = { _, tab -> tab.itemKey() }) { index, tab ->
                val profile = state.profiles[tab.accountKey]
                SwipeToDeleteTabItem(
                    key = tab.itemKey(),
                    onDelete = { tabs.remove(tab) },
                    reorderableLazyColumnState = reorderableLazyColumnState,
                    canSwipeToDelete = true,
                    shapes = ListItemDefaults.segmentedShapes2(index, tabs.size),
                    isEditing = editingTab?.isSameSource(tab) == true,
                    onEdit =
                        if (tab.kind.kagariCanMix()) {
                            { editingTab = tab }
                        } else {
                            null
                        },
                    content = {
                        Text(tab.kind.label())
                    },
                    supportingContent =
                        if (profile != null) {
                            {
                                Text(
                                    text = profile.handle.display,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            null
                        },
                    leadingContent = {
                        AvatarComponent(
                            profile?.avatar,
                            size = 24.dp,
                        )
                    },
                )
            }
        }
    }
    editingTab?.let { tab ->
        KagariSearchTabEditDialog(
            tab = tab,
            onDismissRequest = {
                editingTab = null
            },
            onConfirm = { edited ->
                val index = tabs.indexOfFirst { it.isSameSource(edited) }
                if (index != -1) {
                    tabs[index] = edited
                }
                editingTab = null
            },
        )
    }
    if (showAddTab) {
        KagariSearchTabAddBottomSheet(
            accountOptions = state.accountOptions,
            tabs = tabs,
            onDismissRequest = {
                showAddTab = false
            },
            onAddTab = { tab ->
                if (tabs.none { it.isSameSource(tab) }) {
                    tabs.add(tab)
                }
            },
            onDeleteTab = { tab ->
                tabs.removeAll { it.isSameSource(tab) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KagariSearchTabAddBottomSheet(
    accountOptions: ImmutableList<KagariSearchAccountOption>,
    tabs: List<KagariSearchTab>,
    onDismissRequest: () -> Unit,
    onAddTab: (KagariSearchTab) -> Unit,
    onDeleteTab: (KagariSearchTab) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val pagerState =
                rememberPagerState {
                    accountOptions.size
                }
            val scope = rememberCoroutineScope()
            if (accountOptions.isNotEmpty()) {
                SecondaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    indicator = {
                        TabRowIndicator(
                            selectedIndex = pagerState.currentPage,
                        )
                    },
                    divider = {},
                ) {
                    accountOptions.forEachIndexed { index, account ->
                        AccountLeadingIconTab(
                            profile = account.profile,
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.Top,
            ) { page ->
                val account = accountOptions[page]
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = screenHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(account.kinds) { index, kind ->
                        val tab = account.newTab(kind)
                        val isAdded = tabs.any { it.isSameSource(tab) }
                        ListTabItem(
                            isAdded = isAdded,
                            leadingContent = {
                                AvatarComponent(
                                    account.profile.avatar,
                                    size = 24.dp,
                                )
                            },
                            content = {
                                Text(kind.label())
                            },
                            modifier =
                                Modifier
                                    .listCard(
                                        index = index,
                                        totalCount = account.kinds.size,
                                    ).clickable {
                                        if (isAdded) {
                                            onDeleteTab(tab)
                                        } else {
                                            onAddTab(tab)
                                        }
                                    },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KagariSearchTabEditDialog(
    tab: KagariSearchTab,
    onDismissRequest: () -> Unit,
    onConfirm: (KagariSearchTab) -> Unit,
) {
    var mixed by remember(tab) { mutableStateOf(tab.mixed == true) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(tab.copy(mixed = mixed))
                },
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.edit_tab_title))
        },
        text = {
            SegmentedListItem(
                onClick = { mixed = !mixed },
                shapes = ListItemDefaults.single(),
                content = { Text(text = stringResource(id = R.string.kagari_search_mixed_show)) },
                supportingContent = { Text(text = stringResource(id = R.string.kagari_search_mixed_show_description)) },
                trailingContent = {
                    Switch(
                        checked = mixed,
                        onCheckedChange = { mixed = it },
                    )
                },
            )
        },
    )
}
