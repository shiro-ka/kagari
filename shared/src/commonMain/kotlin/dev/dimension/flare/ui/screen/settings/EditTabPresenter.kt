package dev.dimension.flare.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.model.IconType
import dev.dimension.flare.data.model.asLogo
import dev.dimension.flare.data.model.tab.TimelineResolver
import dev.dimension.flare.data.model.tab.UiSourceTimelineTabItem
import dev.dimension.flare.data.model.tab.UiTimelineTabItem
import dev.dimension.flare.data.repository.AccountRepository
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.ui.model.TabPickerUiIcons
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.UiStrings
import dev.dimension.flare.ui.model.UiText
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public class EditTabPresenter(
    private val tabItem: UiTimelineTabItem,
    private val localizedString: suspend (UiStrings) -> String = { it.name },
) : PresenterBase<EditTabPresenter.State>() {
    public constructor(tabItem: UiTimelineTabItem) : this(tabItem, localizedString = { it.name })

    private val timelineResolver: TimelineResolver by koinInject()
    private val accountRepository: AccountRepository by koinInject()

    public interface State {
        public val availableIcons: ImmutableList<IconType>
        public val initialText: UiState<String>
        public val icon: IconType
        public val mainIcon: IconType
        public val badgeIcon: IconType?
        public val withBadge: Boolean
        public val canUseBadge: Boolean

        public fun setWithBadge(value: Boolean)

        public fun setIcon(value: IconType)

        public fun setBadgeIcon(value: IconType)
    }

    @Composable
    override fun body(): State {
        var initialText: UiState<String> by remember(tabItem) {
            mutableStateOf(UiState.Loading())
        }
        val initialParts = remember(tabItem) { tabItem.icon.splitBadge() }
        var mainIcon: IconType by remember(tabItem) {
            mutableStateOf(initialParts.first)
        }
        var selectedBadge: IconType? by remember(tabItem) {
            mutableStateOf(initialParts.second)
        }
        var withBadge by remember(tabItem) {
            mutableStateOf(initialParts.second != null)
        }
        var platformLogo: IconType? by remember(tabItem) {
            mutableStateOf(null)
        }
        val accountKey =
            remember(tabItem) {
                timelineResolver.resolveAccountKey(tabItem)
            }
        val originalIcons =
            remember(tabItem) {
                listOfNotNull(tabItem.icon, (tabItem as? UiSourceTimelineTabItem)?.source?.icon)
                    .flatMap { icon -> icon.splitBadge().toList().filterNotNull() }
                    .filter { it is IconType.Url || it is IconType.FavIcon }
            }

        LaunchedEffect(tabItem, localizedString) {
            initialText = UiState.Success(tabItem.title.displayText(localizedString))
        }

        LaunchedEffect(accountKey) {
            platformLogo = accountKey?.let { accountRepository.find(it) }?.platformIcon?.asLogo()
        }

        val badgeIcon = if (withBadge) selectedBadge ?: platformLogo else null

        return object : State {
            override val initialText: UiState<String> = initialText
            override val withBadge: Boolean = withBadge
            override val canUseBadge: Boolean = accountKey != null || withBadge
            override val availableIcons: ImmutableList<IconType> =
                (
                    accountKey
                        ?.let {
                            listOf(
                                IconType.Avatar(it),
                                IconType.FavIcon(it.host),
                            )
                        }.orEmpty() +
                        listOfNotNull(platformLogo) +
                        originalIcons +
                        TabPickerUiIcons.map {
                            IconType.Material(it)
                        }
                ).distinct()
                    .toPersistentList()
            override val mainIcon: IconType = mainIcon
            override val badgeIcon: IconType? = badgeIcon
            override val icon: IconType =
                badgeIcon?.let { IconType.Badged(mainIcon, it) } ?: mainIcon

            override fun setWithBadge(value: Boolean) {
                if (value && accountKey == null) return
                if (value && !withBadge) {
                    selectedBadge = null
                }
                withBadge = value
            }

            override fun setIcon(value: IconType) {
                mainIcon = value.splitBadge().first
            }

            override fun setBadgeIcon(value: IconType) {
                selectedBadge = value.splitBadge().first
            }
        }
    }
}

private fun IconType.splitBadge(): Pair<IconType, IconType?> =
    when (this) {
        is IconType.Mixed -> IconType.Avatar(accountKey) to IconType.Material(icon)
        is IconType.Badged -> icon to badge
        else -> this to null
    }

private suspend fun UiText.displayText(localizedString: suspend (UiStrings) -> String): String =
    when (this) {
        is UiText.Localized -> localizedString(string)
        is UiText.Raw -> string
        is UiText.ExternalRef -> fallbackText()
    }
