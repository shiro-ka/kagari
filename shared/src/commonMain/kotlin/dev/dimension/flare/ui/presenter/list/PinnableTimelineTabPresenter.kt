package dev.dimension.flare.ui.presenter.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.map
import dev.dimension.flare.common.PagingState
import dev.dimension.flare.common.toPagingState
import dev.dimension.flare.data.datasource.microblog.capabilities
import dev.dimension.flare.data.datasource.microblog.datasource.PinnableTimelineTabSection
import dev.dimension.flare.data.model.tab.TimelineCandidate
import dev.dimension.flare.data.model.tab.TimelineResolver
import dev.dimension.flare.data.model.tab.UiTimelineTabItem
import dev.dimension.flare.data.repository.AccountRepository
import dev.dimension.flare.data.repository.accountServiceFlow
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.UiText
import dev.dimension.flare.ui.model.collectAsUiState
import dev.dimension.flare.ui.model.map
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

/**
 * Presenter for pinnable lists.
 * Different from [AllListPresenter] in that it also includes Bluesky feeds.
 * This presenter should be used for managing pinnable tabs.
 */
public class PinnableTimelineTabPresenter(
    private val accountType: AccountType,
) : PresenterBase<PinnableTimelineTabPresenter.State>() {
    @Immutable
    public interface State {
        public val tabs: UiState<ImmutableList<PinnableTimelineTab>>
    }

    @Immutable
    public data class PinnableTimelineTab(
        val title: UiText,
        val data: PagingState<UiTimelineTabItem>,
    )

    private val accountRepository: AccountRepository by koinInject()

    private val timelineResolver: TimelineResolver by koinInject()

    private data class AddableSection(
        val title: UiText,
        val tabs: ImmutableList<UiTimelineTabItem>,
    )

    private data class Sections(
        val addableSections: List<AddableSection>,
        val pinnableTabs: List<PinnableTimelineTabSection>,
    )

    private val sectionsFlow by lazy {
        accountServiceFlow(
            accountType = accountType,
            repository = accountRepository,
        ).map { service ->
            val tabCatalog = service.capabilities.tabCatalog
            Sections(
                addableSections =
                    tabCatalog
                        ?.configuration
                        ?.kagariAddableTimelineTabSections
                        .orEmpty()
                        .filter { it.tabs.isNotEmpty() }
                        .map { section ->
                            AddableSection(
                                title = section.title,
                                tabs =
                                    section.tabs
                                        .map<TimelineCandidate<*>, UiTimelineTabItem> { timelineResolver.toNewTabItem(it) }
                                        .toImmutableList(),
                            )
                        },
                pinnableTabs =
                    tabCatalog
                        ?.pinnable
                        ?.pinnableTimelineTabs
                        .orEmpty(),
            )
        }
    }

    @Composable
    override fun body(): State {
        val scope = rememberCoroutineScope()
        val sections by sectionsFlow.collectAsUiState()

        val tabs =
            sections.map { sections ->
                val addableSections =
                    sections.addableSections
                        .map { section ->
                            PinnableTimelineTab(
                                title = section.title,
                                data = PagingState.Success.ImmutableSuccess(section.tabs),
                            )
                        }
                (
                    addableSections +
                        sections.pinnableTabs.map { section ->
                            val pagingState =
                                remember(section) {
                                    section.data
                                        .map { pagingData ->
                                            pagingData.map<TimelineCandidate<*>, UiTimelineTabItem> { timelineResolver.toNewTabItem(it) }
                                        }.cachedIn(scope)
                                }.collectAsLazyPagingItems().toPagingState()
                            PinnableTimelineTab(
                                title = section.title,
                                data = pagingState,
                            )
                        }
                ).toImmutableList()
            }

        return object : State {
            override val tabs = tabs
        }
    }
}
