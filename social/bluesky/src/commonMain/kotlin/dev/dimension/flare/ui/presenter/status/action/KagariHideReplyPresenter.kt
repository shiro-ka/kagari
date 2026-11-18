package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.KagariTooManyHiddenRepliesException
import dev.dimension.flare.data.datasource.bluesky.loadBlueskyReplyVisibility
import dev.dimension.flare.data.datasource.bluesky.setBlueskyReplyHidden
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.kagariLaunch
import dev.dimension.flare.ui.presenter.kagariLoadState
import dev.dimension.flare.ui.presenter.rememberKagariBluesky
import kotlinx.coroutines.CoroutineScope

public class KagariHideReplyPresenter(
    private val accountType: AccountType,
    private val statusKey: MicroBlogKey,
) : PresenterBase<KagariHideReplyState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariHideReplyState {
        val dataSource = accountService.rememberKagariBluesky(accountType)
        var hidden by remember { mutableStateOf<UiState<Boolean>>(UiState.Loading()) }
        var saving by remember { mutableStateOf(false) }
        LaunchedEffect(dataSource) {
            val source = dataSource ?: return@LaunchedEffect
            hidden =
                kagariLoadState {
                    loadBlueskyReplyVisibility(source.kagariService(), source.accountKey, statusKey.id).hidden
                }
        }
        return object : KagariHideReplyState {
            override val hidden: UiState<Boolean> = hidden
            override val saving: Boolean = saving

            override fun setHidden(
                hide: Boolean,
                onResult: (KagariHideReplyState.Result) -> Unit,
            ) {
                val source = dataSource ?: return onResult(KagariHideReplyState.Result.Failed)
                scope.kagariLaunch<Unit>(
                    busy = { saving = it },
                    onDone = { result ->
                        onResult(
                            result.fold(
                                onSuccess = { KagariHideReplyState.Result.Done },
                                onFailure = {
                                    if (it is KagariTooManyHiddenRepliesException) {
                                        KagariHideReplyState.Result.TooMany
                                    } else {
                                        KagariHideReplyState.Result.Failed
                                    }
                                },
                            ),
                        )
                    },
                ) {
                    setBlueskyReplyHidden(source.kagariService(), source.accountKey, statusKey.id, hide)
                }
            }
        }
    }
}

@Immutable
public interface KagariHideReplyState {
    public val hidden: UiState<Boolean>
    public val saving: Boolean

    public fun setHidden(
        hide: Boolean,
        onResult: (Result) -> Unit,
    )

    public enum class Result {
        Done,
        TooMany,
        Failed,
    }
}
