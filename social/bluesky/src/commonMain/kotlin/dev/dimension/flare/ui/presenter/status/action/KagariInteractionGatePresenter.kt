package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.datasource.bluesky.blueskyInteractionGateConfig
import dev.dimension.flare.data.datasource.bluesky.loadBlueskyInteractionGate
import dev.dimension.flare.data.datasource.bluesky.saveBlueskyInteractionGate
import dev.dimension.flare.data.datasource.microblog.InteractionGate
import dev.dimension.flare.data.datasource.microblog.InteractionGateList
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.collectAsUiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

public class KagariInteractionGatePresenter(
    private val accountType: AccountType,
    private val statusKey: MicroBlogKey,
) : PresenterBase<KagariInteractionGateState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()
    private val serviceFlow by lazy {
        accountService.accountServiceFlow(accountType).map { service ->
            require(service is BlueskyDataSource)
            service
        }
    }

    @Composable
    override fun body(): KagariInteractionGateState {
        val serviceState by serviceFlow.collectAsUiState()
        val dataSource = serviceState.takeSuccess()
        var current by remember { mutableStateOf<UiState<InteractionGate>>(UiState.Loading()) }
        var isReply by remember { mutableStateOf(false) }
        var lists by remember { mutableStateOf<List<InteractionGateList>?>(null) }
        var saving by remember { mutableStateOf(false) }
        LaunchedEffect(dataSource) {
            val source = dataSource ?: return@LaunchedEffect
            current =
                runCatching {
                    loadBlueskyInteractionGate(source.kagariService(), source.accountKey, statusKey.id)
                }.fold(
                    onSuccess = {
                        isReply = it.isReply
                        UiState.Success(it.gate)
                    },
                    onFailure = { UiState.Error(it) },
                )
            lists =
                runCatching {
                    blueskyInteractionGateConfig(source.accountKey, source::kagariService).loadLists()
                }.getOrDefault(emptyList())
        }
        return object : KagariInteractionGateState {
            override val current: UiState<InteractionGate> = current
            override val isReply: Boolean = isReply
            override val lists: List<InteractionGateList>? = lists
            override val saving: Boolean = saving

            override fun save(
                gate: InteractionGate,
                onResult: (Boolean) -> Unit,
            ) {
                val source = dataSource ?: return onResult(false)
                saving = true
                scope.launch {
                    val ok =
                        runCatching {
                            saveBlueskyInteractionGate(source.kagariService(), source.accountKey, statusKey.id, gate)
                        }.isSuccess
                    withContext(Dispatchers.Main) {
                        saving = false
                        onResult(ok)
                    }
                }
            }
        }
    }
}

@Immutable
public interface KagariInteractionGateState {
    public val current: UiState<InteractionGate>
    public val isReply: Boolean
    public val lists: List<InteractionGateList>?
    public val saving: Boolean

    public fun save(
        gate: InteractionGate,
        onResult: (Boolean) -> Unit,
    )
}
