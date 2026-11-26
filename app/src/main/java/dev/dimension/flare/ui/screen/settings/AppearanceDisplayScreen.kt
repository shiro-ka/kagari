package dev.dimension.flare.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.data.model.KagariRemoteReactionMode
import dev.dimension.flare.data.model.KagariVerifiedBadge
import dev.dimension.flare.data.model.appearance.AppearanceKeys
import dev.dimension.flare.ui.component.BackButton
import dev.dimension.flare.ui.component.FlareLargeFlexibleTopAppBar
import dev.dimension.flare.ui.component.FlareScaffold
import dev.dimension.flare.ui.component.LocalGlobalAppearance
import dev.dimension.flare.ui.component.LocalTimelineAppearance
import dev.dimension.flare.ui.component.status.StatusItem
import dev.dimension.flare.ui.model.isSuccess
import dev.dimension.flare.ui.model.onSuccess
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.theme.first
import dev.dimension.flare.ui.theme.item
import dev.dimension.flare.ui.theme.last
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import kotlinx.collections.immutable.persistentMapOf
import moe.tlaster.precompose.molecule.producePresenter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppearanceDisplayScreen(onBack: () -> Unit) {
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by producePresenter { appearancePresenter() }
    val timelineAppearance = LocalTimelineAppearance.current
    val globalAppearance = LocalGlobalAppearance.current
    FlareScaffold(
        topBar = {
            FlareLargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.settings_appearance_display_group_title))
                },
                navigationIcon = {
                    BackButton(onBack = onBack)
                },
                scrollBehavior = topAppBarScrollBehavior,
            )
        },
        modifier =
            Modifier
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(it)
                    .padding(horizontal = screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            state.sampleStatus.onSuccess { sample ->
                SegmentedListItem(
                    onClick = {},
                    shapes = ListItemDefaults.first(),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    StatusItem(
                        sample,
                        modifier =
                            Modifier
                                .background(MaterialTheme.colorScheme.surface),
                    )
                }
            }
            SegmentedListItem(
                onClick = {
                    state.update(AppearanceKeys.AbsoluteTimestamp, !timelineAppearance.absoluteTimestamp)
                },
                shapes =
                    if (state.sampleStatus.isSuccess) {
                        ListItemDefaults.item()
                    } else {
                        ListItemDefaults.first()
                    },
                content = {
                    Text(text = stringResource(id = R.string.settings_appearance_absolute_timestamp))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.settings_appearance_absolute_timestamp_description))
                },
                trailingContent = {
                    Switch(
                        checked = timelineAppearance.absoluteTimestamp,
                        onCheckedChange = {
                            state.update(AppearanceKeys.AbsoluteTimestamp, it)
                        },
                    )
                },
            )
            SegmentedListItem(
                onClick = {
                    state.update(AppearanceKeys.ShowPlatformLogo, !timelineAppearance.showPlatformLogo)
                },
                shapes = ListItemDefaults.item(),
                content = {
                    Text(text = stringResource(id = R.string.settings_appearance_show_platform_logo))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.settings_appearance_show_platform_logo_description))
                },
                trailingContent = {
                    Switch(
                        checked = timelineAppearance.showPlatformLogo,
                        onCheckedChange = {
                            state.update(AppearanceKeys.ShowPlatformLogo, it)
                        },
                    )
                },
            )
            SingleChoiceSettingsItem(
                headline = { Text(text = stringResource(id = R.string.kagari_settings_verified_badge)) },
                supporting = { Text(text = stringResource(id = R.string.kagari_settings_verified_badge_description)) },
                items =
                    persistentMapOf(
                        KagariVerifiedBadge.Off to stringResource(id = R.string.kagari_settings_verified_badge_off),
                        KagariVerifiedBadge.Simple to stringResource(id = R.string.kagari_settings_verified_badge_simple),
                        KagariVerifiedBadge.On to stringResource(id = R.string.kagari_settings_verified_badge_on),
                    ),
                selected = globalAppearance.kagariVerifiedBadge,
                onSelected = {
                    state.update(AppearanceKeys.KagariVerifiedBadge, it)
                },
                shapes = ListItemDefaults.item(),
            )
            SegmentedListItem(
                onClick = {
                    state.update(AppearanceKeys.KagariShowHiddenReplies, !globalAppearance.kagariShowHiddenReplies)
                },
                shapes = ListItemDefaults.item(),
                content = {
                    Text(text = stringResource(id = R.string.kagari_settings_show_hidden_replies))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.kagari_settings_show_hidden_replies_description))
                },
                trailingContent = {
                    Switch(
                        checked = globalAppearance.kagariShowHiddenReplies,
                        onCheckedChange = {
                            state.update(AppearanceKeys.KagariShowHiddenReplies, it)
                        },
                    )
                },
            )
            SegmentedListItem(
                onClick = {
                    state.update(AppearanceKeys.KagariSearchTop, !globalAppearance.kagariSearchTop)
                },
                shapes = ListItemDefaults.item(),
                content = {
                    Text(text = stringResource(id = R.string.kagari_settings_search_top))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.kagari_settings_search_top_description))
                },
                trailingContent = {
                    Switch(
                        checked = globalAppearance.kagariSearchTop,
                        onCheckedChange = {
                            state.update(AppearanceKeys.KagariSearchTop, it)
                        },
                    )
                },
            )
            SegmentedListItem(
                onClick = {
                    state.update(AppearanceKeys.KagariShortenLinks, !globalAppearance.kagariShortenLinks)
                },
                shapes = ListItemDefaults.item(),
                content = {
                    Text(text = stringResource(id = R.string.kagari_settings_shorten_links))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.kagari_settings_shorten_links_description))
                },
                trailingContent = {
                    Switch(
                        checked = globalAppearance.kagariShortenLinks,
                        onCheckedChange = {
                            state.update(AppearanceKeys.KagariShortenLinks, it)
                        },
                    )
                },
            )
            SegmentedListItem(
                onClick = {
                    state.update(AppearanceKeys.ShowEmojiReactions, !timelineAppearance.showEmojiReactions)
                },
                shapes = ListItemDefaults.item(),
                content = {
                    Text(text = stringResource(id = R.string.settings_appearance_show_reactions))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.settings_appearance_show_reactions_description))
                },
                trailingContent = {
                    Switch(
                        checked = timelineAppearance.showEmojiReactions,
                        onCheckedChange = {
                            state.update(AppearanceKeys.ShowEmojiReactions, it)
                        },
                    )
                },
            )
            SingleChoiceSettingsItem(
                headline = { Text(text = stringResource(id = R.string.kagari_settings_remote_reactions)) },
                supporting = { Text(text = stringResource(id = R.string.kagari_settings_remote_reactions_description)) },
                items =
                    persistentMapOf(
                        KagariRemoteReactionMode.AsIs to stringResource(id = R.string.kagari_settings_remote_reactions_as_is),
                        KagariRemoteReactionMode.MergeIntoHeart to
                            stringResource(id = R.string.kagari_settings_remote_reactions_merge_into_heart),
                        KagariRemoteReactionMode.Hidden to stringResource(id = R.string.kagari_settings_remote_reactions_hidden),
                    ),
                selected = globalAppearance.kagariRemoteReactionMode,
                onSelected = {
                    state.update(AppearanceKeys.KagariRemoteReactionMode, it)
                },
                shapes = ListItemDefaults.item(),
            )
            SegmentedListItem(
                onClick = {
                    state.update(AppearanceKeys.ShowLinkPreview, !timelineAppearance.showLinkPreview)
                },
                shapes =
                    if (timelineAppearance.showLinkPreview) {
                        ListItemDefaults.item()
                    } else {
                        ListItemDefaults.last()
                    },
                content = {
                    Text(text = stringResource(id = R.string.settings_appearance_show_link_previews))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.settings_appearance_show_link_previews_description))
                },
                trailingContent = {
                    Switch(
                        checked = timelineAppearance.showLinkPreview,
                        onCheckedChange = {
                            state.update(AppearanceKeys.ShowLinkPreview, it)
                        },
                    )
                },
            )
            AnimatedVisibility(visible = timelineAppearance.showLinkPreview) {
                SegmentedListItem(
                    onClick = {
                        state.update(AppearanceKeys.CompatLinkPreview, !timelineAppearance.compatLinkPreview)
                    },
                    shapes = ListItemDefaults.last(),
                    content = {
                        Text(text = stringResource(id = R.string.settings_appearance_compat_link_previews))
                    },
                    supportingContent = {
                        Text(text = stringResource(id = R.string.settings_appearance_compat_link_previews_description))
                    },
                    trailingContent = {
                        Switch(
                            checked = timelineAppearance.compatLinkPreview,
                            onCheckedChange = {
                                state.update(AppearanceKeys.CompatLinkPreview, it)
                            },
                        )
                    },
                )
            }
        }
    }
}
