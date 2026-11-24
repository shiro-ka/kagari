package dev.dimension.flare.ui.screen.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Bars
import compose.icons.fontawesomeicons.solid.EllipsisVertical
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.microblog.ActionMenu
import dev.dimension.flare.data.datasource.microblog.KagariPlatformPostActionLayouts
import dev.dimension.flare.data.datasource.microblog.KagariPostActionPlatforms
import dev.dimension.flare.data.datasource.microblog.PostActionFamily
import dev.dimension.flare.data.datasource.microblog.PostActionLayoutConfig
import dev.dimension.flare.data.datasource.microblog.PostActionLayoutHelpers
import dev.dimension.flare.data.datasource.microblog.PostActionPlacement
import dev.dimension.flare.data.model.appearance.AppearanceKeys
import dev.dimension.flare.model.PlatformRegistry
import dev.dimension.flare.ui.component.BackButton
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.FlareLargeFlexibleTopAppBar
import dev.dimension.flare.ui.component.FlareScaffold
import dev.dimension.flare.ui.component.LocalTimelineAppearance
import dev.dimension.flare.ui.component.resolveText
import dev.dimension.flare.ui.component.status.StatusItem
import dev.dimension.flare.ui.component.toImageVector
import dev.dimension.flare.ui.model.PostActionLayoutPreviewHelper
import dev.dimension.flare.ui.model.UiAccount
import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.settings.AccountsPresenter
import dev.dimension.flare.ui.theme.first
import dev.dimension.flare.ui.theme.last
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import dev.dimension.flare.ui.theme.segmentedShapes2
import dev.dimension.flare.ui.theme.single
import kotlinx.collections.immutable.toPersistentList
import moe.tlaster.precompose.molecule.producePresenter
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PostActionLayoutScreen(onBack: () -> Unit) {
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by producePresenter { appearancePresenter() }
    val accountsState by producePresenter("kagari_post_action_layout_accounts") {
        remember { AccountsPresenter() }.invoke()
    }
    val platformRegistry = koinInject<PlatformRegistry>()
    val timelineAppearance = LocalTimelineAppearance.current
    val platforms =
        remember(accountsState.accounts, platformRegistry) {
            kagariPostActionPlatformsOf(
                accounts =
                    accountsState.accounts
                        .takeSuccess()
                        .orEmpty()
                        .map { it.account },
                order = platformRegistry.all.map { it.platformId },
            )
        }
    var selectedPlatformId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPlatform = platforms.firstOrNull { it.platformId == selectedPlatformId } ?: platforms.firstOrNull()
    val platformId = selectedPlatform?.platformId
    val families = KagariPostActionPlatforms.familiesFor(platformId)
    val persistedLayouts = timelineAppearance.kagariPostActionLayouts
    val persistedConfig =
        PostActionLayoutHelpers.normalizedForEdit(
            persistedLayouts.layoutFor(platformId, timelineAppearance.postActionLayout),
            families,
        )
    val configState = remember { mutableStateOf(persistedConfig) }
    var config by configState
    var isDraggingAction by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(persistedConfig) {
        if (!isDraggingAction) {
            configState.value = persistedConfig
        }
    }

    fun updateConfig(
        value: PostActionLayoutConfig,
        persist: Boolean = true,
    ) {
        val normalized = PostActionLayoutHelpers.normalizedForEdit(value, families)
        configState.value = normalized
        if (persist && normalized != persistedConfig) {
            if (platformId == null) {
                state.update(AppearanceKeys.PostActionLayout, normalized)
            } else {
                state.update(
                    AppearanceKeys.KagariPostActionLayoutByPlatform,
                    persistedLayouts.with(platformId, normalized),
                )
            }
        }
    }

    fun commitConfig() {
        updateConfig(configState.value, persist = true)
    }

    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromFamily = from.key.asPostActionFamilyOrNull() ?: return@rememberReorderableLazyListState
            val toFamily = to.key.asPostActionFamilyOrNull() ?: return@rememberReorderableLazyListState
            val currentConfig = configState.value
            val fromPlacement = PostActionLayoutHelpers.placementOf(currentConfig, fromFamily)
            val toPlacement = PostActionLayoutHelpers.placementOf(currentConfig, toFamily)
            if (fromPlacement == toPlacement) {
                updateConfig(
                    PostActionLayoutHelpers.moveWithin(currentConfig, fromPlacement, fromFamily, toFamily, families),
                    persist = false,
                )
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

    val previewSample =
        state.sampleStatus
            .takeSuccess()
            ?.takeIf { platformId == null || it.platformId == platformId }

    FlareScaffold(
        topBar = {
            FlareLargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.settings_post_action_layout_title))
                },
                navigationIcon = {
                    BackButton(onBack = onBack)
                },
                scrollBehavior = topAppBarScrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.padding(horizontal = screenHorizontalPadding),
        ) {
            if (platforms.isNotEmpty()) {
                item(key = "platform_tabs") {
                    KagariPostActionPlatformTabs(
                        platforms = platforms,
                        selectedPlatformId = platformId,
                        onSelected = {
                            selectedPlatformId = it
                            configState.value =
                                PostActionLayoutHelpers.normalizedForEdit(
                                    persistedLayouts.layoutFor(it, timelineAppearance.postActionLayout),
                                    KagariPostActionPlatforms.familiesFor(it),
                                )
                        },
                    )
                }
            }

            item(key = "preview_and_enabled") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    if (previewSample != null) {
                        SegmentedListItem(
                            onClick = {},
                            shapes = ListItemDefaults.first(),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            CompositionLocalProvider(
                                LocalTimelineAppearance provides
                                    timelineAppearance.copy(
                                        postActionLayout = config,
                                        kagariPostActionLayouts = KagariPlatformPostActionLayouts.Default,
                                    ),
                            ) {
                                StatusItem(
                                    previewSample.withKagariPreviewActions(families),
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                                )
                            }
                        }
                    }

                    SegmentedListItem(
                        onClick = {
                            updateConfig(config.copy(enabled = !config.enabled))
                        },
                        shapes = if (previewSample != null) ListItemDefaults.last() else ListItemDefaults.single(),
                        content = {
                            if (selectedPlatform != null) {
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.kagari_post_action_layout_enable_for_platform,
                                            selectedPlatform.platformDisplayNameText.resolveText(),
                                        ),
                                )
                            } else {
                                Text(text = stringResource(id = R.string.settings_post_action_layout_enable))
                            }
                        },
                        supportingContent = {
                            Text(text = stringResource(id = R.string.settings_post_action_layout_enable_description))
                        },
                        trailingContent = {
                            Switch(
                                checked = config.enabled,
                                onCheckedChange = {
                                    updateConfig(config.copy(enabled = it))
                                },
                            )
                        },
                    )
                }
            }

            if (config.enabled) {
                postActionSection(
                    titleRes = R.string.settings_post_action_layout_button_row,
                    placement = PostActionPlacement.ButtonRow,
                    config = config,
                    platformId = platformId,
                    families = families,
                    onConfigChanged = {
                        updateConfig(it)
                    },
                    onDragStarted = {
                        isDraggingAction = true
                    },
                    onDragStopped = {
                        isDraggingAction = false
                        commitConfig()
                    },
                    reorderableState = reorderableState,
                )
                postActionSection(
                    titleRes = R.string.settings_post_action_layout_more_menu,
                    placement = PostActionPlacement.MoreMenu,
                    config = config,
                    platformId = platformId,
                    families = families,
                    onConfigChanged = {
                        updateConfig(it)
                    },
                    onDragStarted = {
                        isDraggingAction = true
                    },
                    onDragStopped = {
                        isDraggingAction = false
                        commitConfig()
                    },
                    reorderableState = reorderableState,
                )
                postActionSection(
                    titleRes = R.string.settings_post_action_layout_hidden,
                    placement = PostActionPlacement.Hidden,
                    config = config,
                    platformId = platformId,
                    families = families,
                    onConfigChanged = {
                        updateConfig(it)
                    },
                    onDragStarted = {
                        isDraggingAction = true
                    },
                    onDragStopped = {
                        isDraggingAction = false
                        commitConfig()
                    },
                    reorderableState = reorderableState,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KagariPostActionPlatformTabs(
    platforms: List<UiAccount>,
    selectedPlatformId: String?,
    onSelected: (String) -> Unit,
) {
    val selectedIndex = platforms.indexOfFirst { it.platformId == selectedPlatformId }.coerceAtLeast(0)
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        edgePadding = 0.dp,
        divider = {},
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex),
                height = 2.dp,
            )
        },
    ) {
        platforms.forEachIndexed { index, platform ->
            val selected = index == selectedIndex
            Tab(
                selected = selected,
                onClick = { onSelected(platform.platformId) },
                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Text(
                        text = platform.platformDisplayNameText.resolveText(),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                    )
                },
            )
        }
    }
}

private fun kagariPostActionPlatformsOf(
    accounts: List<UiAccount>,
    order: List<String>,
): List<UiAccount> =
    accounts
        .distinctBy { it.platformId }
        .sortedBy { account ->
            order.indexOf(account.platformId).let { if (it == -1) Int.MAX_VALUE else it }
        }

private fun LazyListScope.postActionSection(
    titleRes: Int,
    placement: PostActionPlacement,
    config: PostActionLayoutConfig,
    platformId: String?,
    families: List<PostActionFamily>,
    onConfigChanged: (PostActionLayoutConfig) -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
) {
    val placed = PostActionLayoutHelpers.familiesFor(config, placement)
    item(key = "${placement.name}_title") {
        Text(
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp),
        )
    }
    if (placed.isEmpty()) {
        item(key = "${placement.name}_empty") {
            SegmentedListItem(
                onClick = {},
                shapes = ListItemDefaults.single(),
                content = {
                    Text(text = stringResource(id = R.string.settings_post_action_layout_empty))
                },
            )
        }
    } else {
        itemsIndexed(
            items = placed,
            key = { _, family -> family.saveableKey },
        ) { index, family ->
            PostActionFamilyRow(
                family = family,
                placement = placement,
                index = index,
                totalCount = placed.size,
                config = config,
                platformId = platformId,
                families = families,
                onConfigChanged = onConfigChanged,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped,
                reorderableState = reorderableState,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LazyItemScope.PostActionFamilyRow(
    family: PostActionFamily,
    placement: PostActionPlacement,
    index: Int,
    totalCount: Int,
    config: PostActionLayoutConfig,
    platformId: String?,
    families: List<PostActionFamily>,
    onConfigChanged: (PostActionLayoutConfig) -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
) {
    val haptics = LocalHapticFeedback.current
    val key = family.saveableKey
    ReorderableItem(reorderableState, key = key) { isDragging ->
        val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
        var showMenu by remember { mutableStateOf(false) }
        SegmentedListItem(
            elevation = ListItemDefaults.elevation(elevation),
            selected = isDragging,
            onClick = {
                showMenu = true
            },
            shapes = ListItemDefaults.segmentedShapes2(index, totalCount),
            leadingContent = {
                FAIcon(
                    imageVector = family.kagariIcon(platformId).toImageVector(),
                    contentDescription = null,
                )
            },
            content = {
                Text(text = stringResource(id = family.kagariLabelRes(platformId)))
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        modifier =
                            Modifier.draggableHandle(
                                onDragStarted = {
                                    onDragStarted()
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                onDragStopped = {
                                    onDragStopped()
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                            ),
                        onClick = {},
                    ) {
                        FAIcon(
                            imageVector = FontAwesomeIcons.Solid.Bars,
                            contentDescription = stringResource(id = R.string.tab_settings_drag),
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            FAIcon(
                                imageVector = FontAwesomeIcons.Solid.EllipsisVertical,
                                contentDescription = stringResource(id = R.string.more),
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            enumValues<PostActionPlacement>().forEach { targetPlacement ->
                                if (targetPlacement != placement) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(id = targetPlacement.moveLabelRes))
                                        },
                                        onClick = {
                                            showMenu = false
                                            onConfigChanged(
                                                PostActionLayoutHelpers.moveTo(config, family, targetPlacement, families),
                                            )
                                        },
                                    )
                                }
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(id = R.string.settings_post_action_layout_move_up))
                                },
                                enabled = index > 0,
                                onClick = {
                                    showMenu = false
                                    onConfigChanged(PostActionLayoutHelpers.moveBy(config, family, -1, families))
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(id = R.string.settings_post_action_layout_move_down))
                                },
                                enabled = index < totalCount - 1,
                                onClick = {
                                    showMenu = false
                                    onConfigChanged(PostActionLayoutHelpers.moveBy(config, family, 1, families))
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}

private fun UiTimelineV2.Post.withKagariPreviewActions(families: List<PostActionFamily>): UiTimelineV2.Post {
    val preview = PostActionLayoutPreviewHelper.withPreviewActions(this)
    return preview.copy(actions = preview.actions.filterKagariFamilies(families).toPersistentList())
}

private fun List<ActionMenu>.filterKagariFamilies(families: List<PostActionFamily>): List<ActionMenu> =
    mapNotNull { action ->
        when (action) {
            is ActionMenu.Item -> {
                action.takeIf { it.actionFamily.isKagariFamilyIn(families) }
            }

            is ActionMenu.Group -> {
                if (action.displayItem.actionFamily.isKagariFamilyIn(families)) {
                    action.copy(actions = action.actions.filterKagariFamilies(families).toPersistentList())
                } else {
                    null
                }
            }

            ActionMenu.Divider -> {
                action
            }
        }
    }

private fun PostActionFamily?.isKagariFamilyIn(families: List<PostActionFamily>): Boolean =
    when (this) {
        null -> true
        PostActionFamily.Quote -> PostActionFamily.Repost in families
        else -> this in families
    }

private const val POST_ACTION_FAMILY_KEY_PREFIX = "post_action_family:"

private val PostActionFamily.saveableKey: String
    get() = "$POST_ACTION_FAMILY_KEY_PREFIX$name"

private fun Any?.asPostActionFamilyOrNull(): PostActionFamily? {
    val key = this as? String ?: return null
    if (!key.startsWith(POST_ACTION_FAMILY_KEY_PREFIX)) return null
    val name = key.removePrefix(POST_ACTION_FAMILY_KEY_PREFIX)
    return runCatching { enumValueOf<PostActionFamily>(name) }.getOrNull()
}

private val PostActionPlacement.moveLabelRes: Int
    get() =
        when (this) {
            PostActionPlacement.ButtonRow -> R.string.settings_post_action_layout_move_to_button_row
            PostActionPlacement.MoreMenu -> R.string.settings_post_action_layout_move_to_more_menu
            PostActionPlacement.Hidden -> R.string.settings_post_action_layout_hide_action
        }

private fun PostActionFamily.kagariIcon(platformId: String?): UiIcon =
    when {
        platformId == KagariPostActionPlatforms.PIXIV && this == PostActionFamily.Favorite -> UiIcon.Like
        else -> icon
    }

private fun PostActionFamily.kagariLabelRes(platformId: String?): Int =
    when (platformId) {
        KagariPostActionPlatforms.BLUESKY,
        KagariPostActionPlatforms.XQT,
        KagariPostActionPlatforms.NOSTR,
        -> {
            when (this) {
                PostActionFamily.Repost -> R.string.kagari_post_action_family_repost_quote
                else -> labelRes
            }
        }

        KagariPostActionPlatforms.MASTODON -> {
            when (this) {
                PostActionFamily.Repost -> R.string.kagari_post_action_family_boost_quote
                PostActionFamily.Like -> R.string.settings_post_action_family_favorite
                PostActionFamily.React -> R.string.kagari_post_action_family_emoji_reaction
                else -> labelRes
            }
        }

        KagariPostActionPlatforms.MISSKEY -> {
            when (this) {
                PostActionFamily.Repost -> R.string.kagari_post_action_family_renote_quote
                else -> labelRes
            }
        }

        KagariPostActionPlatforms.VVO -> {
            when (this) {
                PostActionFamily.Repost -> R.string.kagari_post_action_family_forward
                else -> labelRes
            }
        }

        KagariPostActionPlatforms.PIXIV -> {
            when (this) {
                PostActionFamily.Favorite -> R.string.settings_post_action_family_bookmark
                else -> labelRes
            }
        }

        else -> {
            labelRes
        }
    }

private val PostActionFamily.icon: UiIcon
    get() =
        when (this) {
            PostActionFamily.Reply -> UiIcon.Reply
            PostActionFamily.Comment -> UiIcon.Comment
            PostActionFamily.Repost -> UiIcon.Retweet
            PostActionFamily.Quote -> UiIcon.Quote
            PostActionFamily.Like -> UiIcon.Like
            PostActionFamily.React -> UiIcon.React
            PostActionFamily.Translate -> UiIcon.Translate
            PostActionFamily.Bookmark -> UiIcon.Bookmark
            PostActionFamily.Favorite -> UiIcon.Favourite
            PostActionFamily.Share -> UiIcon.Share
            PostActionFamily.FxShare -> UiIcon.Share
            PostActionFamily.Delete -> UiIcon.Delete
            PostActionFamily.Report -> UiIcon.Report
            PostActionFamily.MuteUser -> UiIcon.Mute
            PostActionFamily.BlockUser -> UiIcon.Block
            PostActionFamily.BookmarkPublic -> UiIcon.Like
            PostActionFamily.BookmarkPrivate -> UiIcon.KagariPrivateBookmark
            PostActionFamily.Save -> UiIcon.KagariSave
            PostActionFamily.Follow -> UiIcon.Follow
            PostActionFamily.FollowPrivate -> UiIcon.KagariFollowPrivate
            PostActionFamily.OpenInBrowser -> UiIcon.World
        }

private val PostActionFamily.labelRes: Int
    get() =
        when (this) {
            PostActionFamily.Reply -> R.string.settings_post_action_family_reply
            PostActionFamily.Comment -> R.string.settings_post_action_family_comment
            PostActionFamily.Repost -> R.string.settings_post_action_family_repost
            PostActionFamily.Quote -> R.string.settings_post_action_family_quote
            PostActionFamily.Like -> R.string.settings_post_action_family_like
            PostActionFamily.React -> R.string.settings_post_action_family_react
            PostActionFamily.Translate -> R.string.settings_post_action_family_translate
            PostActionFamily.Bookmark -> R.string.settings_post_action_family_bookmark
            PostActionFamily.Favorite -> R.string.settings_post_action_family_favorite
            PostActionFamily.Share -> R.string.settings_post_action_family_share
            PostActionFamily.FxShare -> R.string.settings_post_action_family_fx_share
            PostActionFamily.Delete -> R.string.settings_post_action_family_delete
            PostActionFamily.Report -> R.string.settings_post_action_family_report
            PostActionFamily.MuteUser -> R.string.settings_post_action_family_mute_user
            PostActionFamily.BlockUser -> R.string.settings_post_action_family_block_user
            PostActionFamily.BookmarkPublic -> R.string.kagari_post_action_family_bookmark_public
            PostActionFamily.BookmarkPrivate -> R.string.kagari_post_action_family_bookmark_private
            PostActionFamily.Save -> R.string.kagari_post_action_family_save
            PostActionFamily.Follow -> R.string.kagari_post_action_family_follow
            PostActionFamily.FollowPrivate -> R.string.kagari_post_action_family_follow_private
            PostActionFamily.OpenInBrowser -> R.string.kagari_post_action_family_open_in_browser
        }
