package dev.dimension.flare.ui.presenter.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.microblog.KagariProfileDraftImages
import dev.dimension.flare.data.datasource.microblog.KagariProfileEditor
import dev.dimension.flare.data.datasource.microblog.KagariProfileImageChanges
import dev.dimension.flare.data.datasource.microblog.MicroblogDataSource
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.kagariLoadState
import dev.dimension.flare.ui.presenter.kagariSubmit
import dev.dimension.flare.ui.presenter.rememberKagariService
import kotlinx.coroutines.CoroutineScope

public abstract class KagariProfileEditPresenterBase<D : KagariProfileDraftImages>(
    private val accountKey: MicroBlogKey,
) : PresenterBase<KagariProfileEditState<D>>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    protected abstract fun editorOf(dataSource: MicroblogDataSource): KagariProfileEditor<D>?

    @Composable
    override fun body(): KagariProfileEditState<D> {
        val dataSource = accountService.rememberKagariService(AccountType.Specific(accountKey))
        val editor = remember(dataSource) { dataSource?.let(::editorOf) }
        var draft by remember { mutableStateOf<UiState<D>>(UiState.Loading()) }
        var saving by remember { mutableStateOf(false) }
        LaunchedEffect(editor) {
            val current = editor ?: return@LaunchedEffect
            draft = kagariLoadState { current.load() }
        }
        return object : KagariProfileEditState<D> {
            override val draft: UiState<D> = draft
            override val saving: Boolean = saving

            override fun save(
                after: D,
                images: KagariProfileImageChanges,
                onResult: (Boolean) -> Unit,
            ) {
                val current = editor ?: return onResult(false)
                val before = draft.takeSuccess() ?: return onResult(false)
                scope.kagariSubmit(busy = { saving = it }, onResult = onResult) {
                    current.save(before, after, images)
                }
            }
        }
    }
}

@Immutable
public interface KagariProfileEditState<D : KagariProfileDraftImages> {
    public val draft: UiState<D>
    public val saving: Boolean

    public fun save(
        after: D,
        images: KagariProfileImageChanges,
        onResult: (Boolean) -> Unit,
    )
}
