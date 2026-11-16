package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.datasource.bluesky.setBlueskyPinnedPost
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.collectAsUiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

public class KagariPinPostPresenter(
    private val accountType: AccountType,
    private val statusKey: MicroBlogKey,
) : PresenterBase<KagariPinPostState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()
    private val serviceFlow by lazy {
        accountService.accountServiceFlow(accountType).map { service ->
            require(service is BlueskyDataSource)
            service
        }
    }

    @Composable
    override fun body(): KagariPinPostState {
        val serviceState by serviceFlow.collectAsUiState()
        val dataSource = serviceState.takeSuccess()
        return object : KagariPinPostState {
            override val ready: Boolean = dataSource != null

            override fun setPinned(
                pin: Boolean,
                onResult: (Boolean) -> Unit,
            ) {
                val source = dataSource ?: return onResult(false)
                scope.launch {
                    val ok =
                        runCatching {
                            setBlueskyPinnedPost(source.kagariService(), source.accountKey, statusKey.id, pin)
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
public interface KagariPinPostState {
    public val ready: Boolean

    public fun setPinned(
        pin: Boolean,
        onResult: (Boolean) -> Unit,
    )
}
