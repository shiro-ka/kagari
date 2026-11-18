package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import app.bsky.graph.MuteThreadRequest
import app.bsky.graph.UnmuteThreadRequest
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.kagariSubmit
import dev.dimension.flare.ui.presenter.rememberKagariBluesky
import kotlinx.coroutines.CoroutineScope
import sh.christian.ozone.api.AtUri

public class KagariMuteThreadPresenter(
    private val accountType: AccountType,
    private val rootUri: String,
) : PresenterBase<KagariMuteThreadState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariMuteThreadState {
        val dataSource = accountService.rememberKagariBluesky(accountType)
        return object : KagariMuteThreadState {
            override val ready: Boolean = dataSource != null

            override fun setMuted(
                mute: Boolean,
                onResult: (Boolean) -> Unit,
            ) {
                val source = dataSource ?: return onResult(false)
                scope.kagariSubmit(onResult = onResult) {
                    val service = source.kagariService()
                    if (mute) {
                        service.muteThread(MuteThreadRequest(root = AtUri(rootUri))).requireResponse()
                    } else {
                        service.unmuteThread(UnmuteThreadRequest(root = AtUri(rootUri))).requireResponse()
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
