package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import app.bsky.graph.MuteThreadRequest
import app.bsky.graph.UnmuteThreadRequest
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.datasource.microblog.KagariPostModerationSource
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.kagariSubmit
import dev.dimension.flare.ui.presenter.rememberKagariService
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
        val service = accountService.rememberKagariService(accountType)
        val dataSource = service as? BlueskyDataSource
        val moderation = service as? KagariPostModerationSource
        return object : KagariMuteThreadState {
            override val ready: Boolean = dataSource != null || moderation != null

            override fun setMuted(
                mute: Boolean,
                onResult: (Boolean) -> Unit,
            ) {
                if (dataSource == null && moderation == null) return onResult(false)
                scope.kagariSubmit(onResult = onResult) {
                    if (dataSource != null) {
                        val bluesky = dataSource.kagariService()
                        if (mute) {
                            bluesky.muteThread(MuteThreadRequest(root = AtUri(rootUri))).requireResponse()
                        } else {
                            bluesky.unmuteThread(UnmuteThreadRequest(root = AtUri(rootUri))).requireResponse()
                        }
                    } else {
                        moderation?.kagariSetThreadMuted(rootUri, mute)
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
