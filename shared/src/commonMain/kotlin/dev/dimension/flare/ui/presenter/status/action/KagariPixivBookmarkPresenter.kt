package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.microblog.datasource.KagariPixivBookmarkDataSource
import dev.dimension.flare.data.datasource.microblog.datasource.KagariPixivBookmarkDetail
import dev.dimension.flare.data.repository.AccountRepository
import dev.dimension.flare.data.repository.accountServiceFlow
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.collectAsUiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

public class KagariPixivBookmarkPresenter(
    private val accountType: AccountType,
    private val statusKey: MicroBlogKey,
) : PresenterBase<KagariPixivBookmarkState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountRepository: AccountRepository by koinInject()

    @Composable
    override fun body(): KagariPixivBookmarkState {
        val flow = remember(accountType) { accountServiceFlow(accountType, accountRepository) }
        val service by flow.collectAsUiState()
        val dataSource = service.takeSuccess() as? KagariPixivBookmarkDataSource
        var detail by remember { mutableStateOf<UiState<KagariPixivBookmarkDetail>>(UiState.Loading()) }
        var saving by remember { mutableStateOf(false) }
        var reloadKey by remember { mutableIntStateOf(0) }
        LaunchedEffect(dataSource, reloadKey) {
            val source = dataSource ?: return@LaunchedEffect
            detail = UiState.Loading()
            detail =
                runCatching { source.kagariPixivBookmarkDetail(statusKey) }
                    .fold(
                        onSuccess = { UiState.Success(it) },
                        onFailure = { UiState.Error(it) },
                    )
        }

        fun submit(
            onResult: (Boolean) -> Unit,
            action: suspend (KagariPixivBookmarkDataSource) -> Unit,
        ) {
            val source = dataSource ?: return onResult(false)
            saving = true
            scope.launch {
                val result = runCatching { action(source) }
                withContext(Dispatchers.Main) {
                    saving = false
                    onResult(result.isSuccess)
                }
            }
        }

        return object : KagariPixivBookmarkState {
            override val detail: UiState<KagariPixivBookmarkDetail> = detail
            override val saving: Boolean = saving

            override fun retry() {
                reloadKey++
            }

            override fun save(
                isPrivate: Boolean,
                tags: List<String>,
                onResult: (Boolean) -> Unit,
            ) {
                submit(onResult) { it.kagariPixivSaveBookmark(statusKey, isPrivate, tags) }
            }

            override fun remove(onResult: (Boolean) -> Unit) {
                submit(onResult) { it.kagariPixivRemoveBookmark(statusKey) }
            }
        }
    }
}

@Immutable
public interface KagariPixivBookmarkState {
    public val detail: UiState<KagariPixivBookmarkDetail>
    public val saving: Boolean

    public fun retry()

    public fun save(
        isPrivate: Boolean,
        tags: List<String>,
        onResult: (Boolean) -> Unit,
    )

    public fun remove(onResult: (Boolean) -> Unit)
}
