package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.datasource.bluesky.setBlueskyPinnedPost
import dev.dimension.flare.data.datasource.microblog.KagariPostModerationSource
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.kagariSubmit
import dev.dimension.flare.ui.presenter.rememberKagariService
import kotlinx.coroutines.CoroutineScope

public class KagariPinPostPresenter(
    private val accountType: AccountType,
    private val statusKey: MicroBlogKey,
) : PresenterBase<KagariPinPostState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariPinPostState {
        val service = accountService.rememberKagariService(accountType)
        val dataSource = service as? BlueskyDataSource
        val moderation = service as? KagariPostModerationSource
        return object : KagariPinPostState {
            override val ready: Boolean = dataSource != null || moderation != null

            override fun setPinned(
                pin: Boolean,
                onResult: (Boolean) -> Unit,
            ) {
                if (dataSource == null && moderation == null) return onResult(false)
                scope.kagariSubmit(onResult = onResult) {
                    if (dataSource != null) {
                        setBlueskyPinnedPost(dataSource.kagariService(), dataSource.accountKey, statusKey.id, pin)
                    } else {
                        moderation?.kagariSetPinned(statusKey, pin)
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
