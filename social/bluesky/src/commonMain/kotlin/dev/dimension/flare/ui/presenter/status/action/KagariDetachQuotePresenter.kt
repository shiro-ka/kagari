package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.datasource.bluesky.setBlueskyQuoteDetached
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.collectAsUiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

public class KagariDetachQuotePresenter(
    private val accountType: AccountType,
    private val statusKey: MicroBlogKey,
    private val quotedUri: String,
) : PresenterBase<KagariDetachQuoteState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariDetachQuoteState {
        val flow = remember(accountType) { accountService.accountServiceFlow(accountType) }
        val service by flow.collectAsUiState()
        val bluesky = service.takeSuccess() as? BlueskyDataSource
        var saving by remember { mutableStateOf(false) }
        return object : KagariDetachQuoteState {
            override val ready: Boolean = bluesky != null
            override val saving: Boolean = saving

            override fun setDetached(
                detach: Boolean,
                onResult: (Boolean) -> Unit,
            ) {
                val dataSource = bluesky ?: return onResult(false)
                saving = true
                scope.launch {
                    val ok =
                        runCatching {
                            setBlueskyQuoteDetached(
                                service = dataSource.kagariService(),
                                accountKey = dataSource.accountKey,
                                quotedPostUri = quotedUri,
                                quotingPostUri = statusKey.id,
                                detach = detach,
                            )
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
public interface KagariDetachQuoteState {
    public val ready: Boolean
    public val saving: Boolean

    public fun setDetached(
        detach: Boolean,
        onResult: (Boolean) -> Unit,
    )
}
