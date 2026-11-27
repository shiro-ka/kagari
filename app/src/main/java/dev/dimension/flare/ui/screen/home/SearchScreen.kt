package dev.dimension.flare.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Robot
import dev.dimension.flare.R
import dev.dimension.flare.common.isRefreshing
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.FlareScaffold
import dev.dimension.flare.ui.component.LocalTimelineAppearance
import dev.dimension.flare.ui.component.RefreshContainer
import dev.dimension.flare.ui.component.SearchBar
import dev.dimension.flare.ui.component.SearchBarState
import dev.dimension.flare.ui.component.searchBarPresenter
import dev.dimension.flare.ui.component.status.LazyStatusVerticalStaggeredGrid
import dev.dimension.flare.ui.presenter.home.KagariSearchTabsPresenter
import dev.dimension.flare.ui.presenter.invoke
import kotlinx.coroutines.launch
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun SearchScreen(
    initialQuery: String,
    accountType: AccountType,
    onAskAiClick: (String?) -> Unit,
    toSearchTabSettings: () -> Unit,
) {
    val state by producePresenter("search_${accountType}_$initialQuery") {
        presenter(
            accountType,
            initialQuery,
        )
    }
    val lazyListState = rememberLazyStaggeredGridState()
    val showAskAi = LocalTimelineAppearance.current.aiConfig.agent
    RegisterTabCallback(
        lazyListState = lazyListState,
        onRefresh = {
            state.refresh()
        },
    )
    FlareScaffold(
        topBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SearchBar(
                    state = state,
                    onSearch = {
                        state.commitSearch(it)
                    },
                    historyFloatingActionButton =
                        if (showAskAi) {
                            {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        onAskAiClick(
                                            state.queryTextState
                                                .text
                                                .toString()
                                                .trim()
                                                .takeIf { it.isNotEmpty() },
                                        )
                                    },
                                    icon = {
                                        FAIcon(
                                            imageVector = FontAwesomeIcons.Solid.Robot,
                                            contentDescription = null,
                                        )
                                    },
                                    text = {
                                        Text(text = stringResource(id = R.string.ask_ai))
                                    },
                                )
                            }
                        } else {
                            null
                        },
                )
                KagariSearchTabRow(
                    state = state.searchState,
                    onEdit = toSearchTabSettings,
                )
            }
        },
    ) { contentPadding ->
        RefreshContainer(
            modifier =
                Modifier
                    .fillMaxSize(),
            indicatorPadding = contentPadding,
            isRefreshing = state.refreshing,
            onRefresh = state::refresh,
            content = {
                LazyStatusVerticalStaggeredGrid(
                    state = lazyListState,
                    contentPadding = contentPadding,
                ) {
                    kagariSearchTabContent(state.searchState)
                }
            },
        )
    }
}

@Composable
private fun presenter(
    accountType: AccountType,
    initialQuery: String,
) = run {
    val scope = rememberCoroutineScope()
    val searchBarState = searchBarPresenter(initialQuery)
    val searchState =
        remember(initialQuery, accountType) {
            KagariSearchTabsPresenter(
                preferredAccountKey = (accountType as? AccountType.Specific)?.accountKey,
                initialQuery = initialQuery,
            )
        }.invoke()

    object : SearchBarState by searchBarState {
        val searchState = searchState

        val refreshing =
            searchState.users.isRefreshing ||
                searchState.status.isRefreshing

        fun refresh() {
            scope.launch { searchState.refreshSuspend() }
        }

        fun commitSearch(new: String) {
            searchBarState.setQuery(new)
            searchBarState.addSearchHistory(new)
            searchState.search(new)
        }
    }
}
