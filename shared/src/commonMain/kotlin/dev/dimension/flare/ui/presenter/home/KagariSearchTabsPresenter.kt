package dev.dimension.flare.ui.presenter.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import dev.dimension.flare.common.PagingState
import dev.dimension.flare.common.isEmpty
import dev.dimension.flare.common.refreshSuspend
import dev.dimension.flare.common.toPagingState
import dev.dimension.flare.data.datasource.microblog.KagariSearchKind
import dev.dimension.flare.data.datasource.microblog.KagariSortableSearch
import dev.dimension.flare.data.datasource.microblog.MicroblogDataSource
import dev.dimension.flare.data.datasource.microblog.accountKeyOrNull
import dev.dimension.flare.data.datasource.microblog.accountProfileFlow
import dev.dimension.flare.data.datasource.microblog.kagariSearchKindList
import dev.dimension.flare.data.datasource.microblog.kagariSearchPosts
import dev.dimension.flare.data.datasource.microblog.paging.toPagingSource
import dev.dimension.flare.data.datasource.microblog.pagingConfig
import dev.dimension.flare.data.datastore.AppDataStore
import dev.dimension.flare.data.datastore.model.KagariSearchTab
import dev.dimension.flare.data.repository.AccountRepository
import dev.dimension.flare.data.repository.SettingsRepository
import dev.dimension.flare.data.repository.allAccountServicesFlow
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Immutable
public data class KagariSearchTabItem(
    val tab: KagariSearchTab,
    val profile: UiProfile,
)

@Immutable
public data class KagariSearchAccountOption(
    val profile: UiProfile,
    val kinds: ImmutableList<KagariSearchKind>,
)

@Immutable
public interface KagariSearchTabsState {
    public val tabs: ImmutableList<KagariSearchTabItem>
    public val accountOptions: ImmutableList<KagariSearchAccountOption>
    public val selectedIndex: Int
    public val selectedTab: KagariSearchTabItem?
    public val users: PagingState<UiProfile>
    public val status: PagingState<UiTimelineV2>
    public val searching: Boolean
    public val hasNoResults: Boolean

    public fun select(index: Int)

    public fun search(query: String)

    public fun saveTabs(tabs: List<KagariSearchTab>)

    public suspend fun refreshSuspend()
}

@OptIn(ExperimentalCoroutinesApi::class)
public class KagariSearchTabsPresenter(
    private val preferredAccountKey: MicroBlogKey? = null,
    private val initialQuery: String = "",
) : PresenterBase<KagariSearchTabsState>() {
    private val accountRepository: AccountRepository by koinInject()
    private val settingsRepository: SettingsRepository by koinInject()
    private val appDataStore: AppDataStore by koinInject()
    private val appScope: CoroutineScope by koinInject()
    private val queryFlow = MutableStateFlow(initialQuery)
    private val selectedTabFlow = MutableStateFlow<KagariSearchTab?>(null)

    private data class SearchAccount(
        val key: MicroBlogKey,
        val source: MicroblogDataSource,
        val profile: UiProfile,
        val kinds: List<KagariSearchKind>,
    )

    private data class Resolved(
        val accounts: List<SearchAccount> = emptyList(),
        val tabs: ImmutableList<KagariSearchTabItem> = persistentListOf(),
    )

    private val accountsFlow: Flow<List<SearchAccount>> by lazy {
        allAccountServicesFlow(accountRepository)
            .flatMapLatest { sources ->
                val flows =
                    sources.mapNotNull { source ->
                        val key = source.accountKeyOrNull ?: return@mapNotNull null
                        val kinds = source.kagariSearchKindList()
                        if (kinds.isEmpty()) return@mapNotNull null
                        source.accountProfileFlow()?.map { state ->
                            state.takeSuccess()?.let { profile -> SearchAccount(key, source, profile, kinds) }
                        }
                    }
                if (flows.isEmpty()) {
                    flowOf(emptyList<SearchAccount>())
                } else {
                    combine(flows) { accounts -> accounts.filterNotNull() }
                }
            }.distinctUntilChanged()
    }

    private val resolvedFlow: Flow<Resolved> by lazy {
        combine(
            accountsFlow,
            appDataStore.kagariSearchTabsStore.data,
            settingsRepository.globalAppearance.map { it.kagariSearchTop }.distinctUntilChanged(),
        ) { accounts, preferences, top ->
            val byKey = accounts.associateBy { it.key }
            val saved =
                if (preferences.initialized) {
                    preferences.tabs.mapNotNull { tab ->
                        byKey[tab.accountKey]
                            ?.takeIf { tab.kind in it.kinds }
                            ?.let { KagariSearchTabItem(tab, it.profile) }
                    }
                } else {
                    emptyList()
                }
            val tabs =
                saved.ifEmpty {
                    accounts.map { account ->
                        KagariSearchTabItem(KagariSearchTab(account.key, account.defaultKind(top)), account.profile)
                    }
                }
            Resolved(accounts, tabs.distinctBy { it.tab }.toImmutableList())
        }
    }

    private fun SearchAccount.defaultKind(top: Boolean): KagariSearchKind =
        when {
            source is KagariSortableSearch && KagariSearchKind.Top in kinds && KagariSearchKind.Latest in kinds -> {
                if (top) KagariSearchKind.Top else KagariSearchKind.Latest
            }

            else -> {
                kinds.firstOrNull { it != KagariSearchKind.Users } ?: kinds.first()
            }
        }

    @Composable
    override fun body(): KagariSearchTabsState {
        val resolved by resolvedFlow.collectAsState(Resolved())
        val query by queryFlow.collectAsState()
        val selectedTab by selectedTabFlow.collectAsState()
        val tabs = resolved.tabs
        val selectedIndex =
            tabs.indexOfFirst { it.tab == selectedTab }.takeIf { it >= 0 }
                ?: tabs.indexOfFirst { it.tab.accountKey == preferredAccountKey }.takeIf { it >= 0 }
                ?: 0
        val current = tabs.getOrNull(selectedIndex)
        val account = current?.let { item -> resolved.accounts.firstOrNull { it.key == item.tab.accountKey } }
        val searching = query.isNotEmpty()
        val accountOptions =
            remember(resolved.accounts) {
                resolved.accounts
                    .map { KagariSearchAccountOption(it.profile, it.kinds.toImmutableList()) }
                    .toImmutableList()
            }

        return key(query) {
            val scope = rememberCoroutineScope()
            val statusPagers = remember { mutableMapOf<KagariSearchTab, Flow<PagingData<UiTimelineV2>>>() }
            val userPagers = remember { mutableMapOf<KagariSearchTab, Flow<PagingData<UiProfile>>>() }
            key(current?.tab) {
                val tab = current?.tab
                val isUsers = tab?.kind == KagariSearchKind.Users
                val users =
                    if (searching && tab != null && account != null && isUsers) {
                        userPagers
                            .getOrPut(tab) {
                                Pager(config = pagingConfig) { account.source.searchUser(query).toPagingSource() }
                                    .flow
                                    .cachedIn(scope)
                            }.collectAsLazyPagingItems()
                            .toPagingState()
                    } else {
                        PagingState.Empty {}
                    }
                val status =
                    if (searching && tab != null && account != null && !isUsers) {
                        statusPagers
                            .getOrPut(tab) {
                                TimelinePresenter(
                                    loader = flow { emit(account.source.kagariSearchPosts(query, tab.kind)) },
                                ).createPager(scope)
                            }.collectAsLazyPagingItems()
                            .toPagingState()
                    } else {
                        PagingState.Empty {}
                    }

                object : KagariSearchTabsState {
                    override val tabs = tabs
                    override val accountOptions = accountOptions
                    override val selectedIndex = selectedIndex
                    override val selectedTab = current
                    override val users = users
                    override val status = status
                    override val searching = searching
                    override val hasNoResults = searching && current != null && users.isEmpty && status.isEmpty

                    override fun select(index: Int) {
                        selectedTabFlow.value = tabs.getOrNull(index)?.tab
                    }

                    override fun search(query: String) {
                        queryFlow.value = query
                    }

                    override fun saveTabs(tabs: List<KagariSearchTab>) {
                        appScope.launch {
                            appDataStore.kagariSearchTabsStore.updateData {
                                it.copy(initialized = true, tabs = tabs.distinct())
                            }
                        }
                    }

                    override suspend fun refreshSuspend() {
                        users.refreshSuspend()
                        status.refreshSuspend()
                    }
                }
            }
        }
    }
}
