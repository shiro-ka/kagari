package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import app.bsky.graph.MuteThreadRequest
import app.bsky.graph.UnmuteThreadRequest
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.collectAsUiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.christian.ozone.api.AtUri

public class KagariMuteThreadPresenter(
    private val accountType: AccountType,
    private val rootUri: String,
) : PresenterBase<KagariMuteThreadState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()
    private val serviceFlow by lazy {
        accountService.accountServiceFlow(accountType).map { service ->
            require(service is BlueskyDataSource)
            service
        }
    }

    @Composable
    override fun body(): KagariMuteThreadState {
        val serviceState by serviceFlow.collectAsUiState()
        val dataSource = serviceState.takeSuccess()
        return object : KagariMuteThreadState {
            override val ready: Boolean = dataSource != null

            override fun setMuted(
                mute: Boolean,
                onResult: (Boolean) -> Unit,
            ) {
                val source = dataSource ?: return onResult(false)
                scope.launch {
                    val ok =
                        runCatching {
                            val service = source.kagariService()
                            if (mute) {
                                service.muteThread(MuteThreadRequest(root = AtUri(rootUri))).requireResponse()
                            } else {
                                service.unmuteThread(UnmuteThreadRequest(root = AtUri(rootUri))).requireResponse()
                            }
                        }.isSuccess
                    withContext(Dispatchers.Main) {
                        onResult(ok)
                    }
                }
            }
        }
    }
}

@Immutable
public interface KagariMuteThreadState {
    public val ready: Boolean

    public fun setMuted(
        mute: Boolean,
        onResult: (Boolean) -> Unit,
    )
}
