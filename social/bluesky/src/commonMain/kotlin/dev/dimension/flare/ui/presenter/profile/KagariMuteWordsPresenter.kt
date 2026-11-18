package dev.dimension.flare.ui.presenter.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.KagariMuteWord
import dev.dimension.flare.data.datasource.bluesky.kagariLoadMuteWords
import dev.dimension.flare.data.datasource.bluesky.kagariSaveMuteWords
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.kagariLoadState
import dev.dimension.flare.ui.presenter.kagariSubmit
import dev.dimension.flare.ui.presenter.rememberKagariBluesky
import kotlinx.coroutines.CoroutineScope

public class KagariMuteWordsPresenter(
    private val accountKey: MicroBlogKey,
) : PresenterBase<KagariMuteWordsState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariMuteWordsState {
        val bluesky = accountService.rememberKagariBluesky(AccountType.Specific(accountKey))
        var words by remember { mutableStateOf<UiState<List<KagariMuteWord>>>(UiState.Loading()) }
        var saving by remember { mutableStateOf(false) }
        LaunchedEffect(bluesky) {
            val source = bluesky ?: return@LaunchedEffect
            words = kagariLoadState { source.kagariService().kagariLoadMuteWords(accountKey) }
        }

        fun save(
            next: List<KagariMuteWord>,
            onResult: (Boolean) -> Unit,
        ) {
            val source = bluesky ?: return onResult(false)
            scope.kagariSubmit(
                busy = { saving = it },
                onResult = { ok ->
                    if (ok) words = UiState.Success(next)
                    onResult(ok)
                },
            ) {
                source.kagariService().kagariSaveMuteWords(accountKey, next)
            }
        }
        return object : KagariMuteWordsState {
            override val words: UiState<List<KagariMuteWord>> = words
            override val saving: Boolean = saving

            override fun add(
                word: KagariMuteWord,
                onResult: (Boolean) -> Unit,
            ) {
                val current = words.takeSuccess() ?: return onResult(false)
                save(current + word, onResult)
            }

            override fun remove(
                word: KagariMuteWord,
                onResult: (Boolean) -> Unit,
            ) {
                val current = words.takeSuccess() ?: return onResult(false)
                save(current - word, onResult)
            }
        }
    }
}

@Immutable
public interface KagariMuteWordsState {
    public val words: UiState<List<KagariMuteWord>>
    public val saving: Boolean

    public fun add(
        word: KagariMuteWord,
        onResult: (Boolean) -> Unit,
    )

    public fun remove(
        word: KagariMuteWord,
        onResult: (Boolean) -> Unit,
    )
}
