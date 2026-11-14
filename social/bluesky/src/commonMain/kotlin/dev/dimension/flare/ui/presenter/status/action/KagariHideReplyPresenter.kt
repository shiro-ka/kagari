package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.datasource.bluesky.KagariTooManyHiddenRepliesException
import dev.dimension.flare.data.datasource.bluesky.loadBlueskyReplyVisibility
import dev.dimension.flare.data.datasource.bluesky.setBlueskyReplyHidden
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

public class KagariHideReplyPresenter(
    private val accountType: AccountType,
    private val statusKey: MicroBlogKey,
) : PresenterBase<KagariHideReplyState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()
    private val serviceFlow by lazy {
        accountService.accountServiceFlow(accountType).map { service ->
            require(service is BlueskyDataSource)
            service
        }
    }

    @Composable
    override fun body(): KagariHideReplyState {
        val serviceState by serviceFlow.collectAsUiState()
        val dataSource = serviceState.takeSuccess()
        var hidden by remember { mutableStateOf<UiState<Boolean>>(UiState.Loading()) }
        var saving by remember { mutableStateOf(false) }
        LaunchedEffect(dataSource) {
            val source = dataSource ?: return@LaunchedEffect
            hidden =
                runCatching {
                    loadBlueskyReplyVisibility(source.kagariService(), source.accountKey, statusKey.id).hidden
                }.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it) },
                )
        }
        return object : KagariHideReplyState {
            override val hidden: UiState<Boolean> = hidden
            override val saving: Boolean = saving

            override fun setHidden(
                hide: Boolean,
                onResult: (KagariHideReplyState.Result) -> Unit,
            ) {
                val source = dataSource ?: return onResult(KagariHideReplyState.Result.Failed)
                saving = true
                scope.launch {
                    val result =
                        runCatching {
                            setBlueskyReplyHidden(source.kagariService(), source.accountKey, statusKey.id, hide)
                        }.fold(
                            onSuccess = { KagariHideReplyState.Result.Done },
                            onFailure = {
                                if (it is KagariTooManyHiddenRepliesException) {
                                    KagariHideReplyState.Result.TooMany
                                } else {
                                    KagariHideReplyState.Result.Failed
                                }
                            },
                        )
                    withContext(Dispatchers.Main) {
                        saving = false
                        onResult(result)
                    }
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
