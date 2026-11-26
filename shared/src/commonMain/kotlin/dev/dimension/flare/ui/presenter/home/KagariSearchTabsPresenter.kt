package dev.dimension.flare.ui.presenter.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
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
import dev.dimension.flare.data.datasource.microblog.MicroblogDataSource
import dev.dimension.flare.data.datasource.microblog.accountKeyOrNull
import dev.dimension.flare.data.datasource.microblog.accountProfileFlow
import dev.dimension.flare.data.datasource.microblog.kagariSearchKindList
import dev.dimension.flare.data.datasource.microblog.kagariSearchPosts
import dev.dimension.flare.data.datasource.microblog.paging.toPagingSource
import dev.dimension.flare.data.datasource.microblog.pagingConfig
import dev.dimension.flare.data.datastore.AppDataStore
import dev.dimension.flare.data.datastore.model.KagariSearchTab
import dev.dimension.flare.data.datastore.model.KagariSearchTabPreferences
import dev.dimension.flare.data.repository.AccountRepository
import dev.dimension.flare.data.repository.allAccountServicesFlow
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    public val selectedIndex: Int
    public val selectedTab: KagariSearchTabItem?
    public val users: PagingState<UiProfile>
    public val status: PagingState<UiTimelineV2>
    public val searching: Boolean
    public val hasNoResults: Boolean

    public fun select(index: Int)

    public fun search(query: String)

    public suspend fun refreshSuspend()
}

@OptIn(ExperimentalCoroutinesApi::class)
public class KagariSearchTabsPresenter(
    private val preferredAccountKey: MicroBlogKey? = null,
    private val initialQuery: String = "",
) : PresenterBase<KagariSearchTabsState>() {
    private val accountRepository: AccountRepository by koinInject()
    private val appDataStore: AppDataStore by koinInject()
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
            kagariSearchAccountKindsFlow(accountRepository),
            appDataStore.kagariSearchTabsStore.data,
        ) { accounts, kindAccounts, preferences ->
            val byKey = accounts.associateBy { it.key }
            val tabs =
                preferences.kagariVisibleTabs(kindAccounts).mapNotNull { tab ->
                    byKey[tab.accountKey]
                        ?.takeIf { tab.kind in it.kinds }
                        ?.let { KagariSearchTabItem(tab, it.profile) }
                }
            Resolved(accounts, tabs.toImmutableList())
        }
    }

    @Composable
    override fun body(): KagariSearchTabsState {
        LaunchedEffect(Unit) {
            kagariSearchAccountKindsFlow(accountRepository).collectLatest { accounts ->
                appDataStore.kagariSearchTabsStore.updateData { it.withAccounts(accounts) }
            }
        }
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

                    override suspend fun refreshSuspend() {
                        users.refreshSuspend()
                        status.refreshSuspend()
                    }
                }
            }
        }
    }
}

internal data class KagariSearchAccountKinds(
    val key: MicroBlogKey,
    val kinds: List<KagariSearchKind>,
)

internal fun kagariSearchAccountKindsFlow(accountRepository: AccountRepository): Flow<List<KagariSearchAccountKinds>> =
    allAccountServicesFlow(accountRepository)
        .map { sources ->
            sources.mapNotNull { source ->
                val key = source.accountKeyOrNull ?: return@mapNotNull null
                source
                    .kagariSearchKindList()
                    .takeIf { it.isNotEmpty() }
                    ?.let { KagariSearchAccountKinds(key, it) }
            }
        }.distinctUntilChanged()

internal fun List<KagariSearchKind>.kagariDefaultSearchKind(): KagariSearchKind =
    when {
        KagariSearchKind.Latest in this -> KagariSearchKind.Latest
        else -> firstOrNull { it != KagariSearchKind.Users } ?: first()
    }

internal fun KagariSearchTabPreferences.withAccounts(accounts: List<KagariSearchAccountKinds>): KagariSearchTabPreferences {
    if (!initialized) return this
    if (!accountsTracked) {
        return copy(
            accountsTracked = true,
            knownAccounts = (knownAccounts + tabs.map { it.accountKey } + accounts.map { it.key }).distinct(),
        )
    }
    val added = accounts.filter { it.key !in knownAccounts }
    if (added.isEmpty()) return this
    return copy(
        tabs = (tabs + added.map { KagariSearchTab(it.key, it.kinds.kagariDefaultSearchKind()) }).distinct(),
        knownAccounts = (knownAccounts + added.map { it.key }).distinct(),
    )
}

internal fun KagariSearchTabPreferences.kagariVisibleTabs(accounts: List<KagariSearchAccountKinds>): List<KagariSearchTab> {
    val byKey = accounts.associateBy { it.key }
    val synced = withAccounts(accounts)
    val saved =
        if (synced.initialized) {
            synced.tabs.filter { tab -> byKey[tab.accountKey]?.kinds?.contains(tab.kind) == true }
        } else {
            emptyList()
        }
    return saved
        .ifEmpty { accounts.map { KagariSearchTab(it.key, it.kinds.kagariDefaultSearchKind()) } }
        .distinct()
}

@Immutable
public interface KagariSearchTabsEditState {
    public val loaded: Boolean
    public val tabs: ImmutableList<KagariSearchTab>
    public val profiles: ImmutableMap<MicroBlogKey, UiProfile>
    public val accountOptions: ImmutableList<KagariSearchAccountOption>

    public fun save(tabs: List<KagariSearchTab>)
}

@OptIn(ExperimentalCoroutinesApi::class)
public class KagariSearchTabsEditPresenter : PresenterBase<KagariSearchTabsEditState>() {
    private val accountRepository: AccountRepository by koinInject()
    private val appDataStore: AppDataStore by koinInject()
    private val appScope: CoroutineScope by koinInject()

    private data class Loaded(
        val accounts: List<KagariSearchAccountKinds>,
        val tabs: ImmutableList<KagariSearchTab>,
    )

    private val loadedFlow: Flow<Loaded> by lazy {
        combine(
            kagariSearchAccountKindsFlow(accountRepository),
            appDataStore.kagariSearchTabsStore.data,
        ) { accounts, preferences ->
            Loaded(accounts, preferences.kagariVisibleTabs(accounts).toImmutableList())
        }
    }

    private val profilesFlow: Flow<ImmutableMap<MicroBlogKey, UiProfile>> by lazy {
        allAccountServicesFlow(accountRepository)
            .flatMapLatest { sources ->
                val flows =
                    sources.mapNotNull { source ->
                        val key = source.accountKeyOrNull ?: return@mapNotNull null
                        source.accountProfileFlow()?.map { state -> key to state.takeSuccess() }
                    }
                if (flows.isEmpty()) {
                    flowOf(persistentMapOf())
                } else {
                    combine(flows) { pairs ->
                        pairs
                            .mapNotNull { (key, profile) -> profile?.let { key to it } }
                            .toMap()
                            .toImmutableMap()
                    }
                }
            }.distinctUntilChanged()
    }

    @Composable
    override fun body(): KagariSearchTabsEditState {
        val loadedState by loadedFlow.collectAsState(null)
        val profiles by profilesFlow.collectAsState(persistentMapOf())
        val current = loadedState
        val accountOptions =
            remember(current?.accounts, profiles) {
                current
                    ?.accounts
                    .orEmpty()
                    .mapNotNull { account ->
                        profiles[account.key]?.let { KagariSearchAccountOption(it, account.kinds.toImmutableList()) }
                    }.toImmutableList()
            }
        return object : KagariSearchTabsEditState {
            override val loaded = current != null
            override val tabs = current?.tabs ?: persistentListOf()
            override val profiles = profiles
            override val accountOptions = accountOptions

            override fun save(tabs: List<KagariSearchTab>) {
                val accounts = current?.accounts ?: return
                val currentKeys = accounts.map { it.key }.toSet()
                appScope.launch {
                    appDataStore.kagariSearchTabsStore.updateData { preferences ->
                        val synced = preferences.withAccounts(accounts)
                        synced.copy(
                            initialized = true,
                            accountsTracked = true,
                            tabs = (tabs + synced.tabs.filter { it.accountKey !in currentKeys }).distinct(),
                            knownAccounts = (synced.knownAccounts + currentKeys).distinct(),
                        )
                    }
                }
            }
        }
    }
}
