package dev.dimension.flare.ui.presenter.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.KagariProfileDraft
import dev.dimension.flare.data.datasource.bluesky.KagariProfileImage
import dev.dimension.flare.data.datasource.bluesky.kagariLoadProfileDraft
import dev.dimension.flare.data.datasource.bluesky.kagariSaveProfile
import dev.dimension.flare.data.datasource.bluesky.kagariSupportsProfileEdit
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

public class KagariProfileEditPresenter(
    private val accountKey: MicroBlogKey,
) : PresenterBase<KagariProfileEditState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariProfileEditState {
        val dataSource = accountService.rememberKagariService(AccountType.Specific(accountKey))?.takeIf { it.kagariSupportsProfileEdit() }
        var draft by remember { mutableStateOf<UiState<KagariProfileDraft>>(UiState.Loading()) }
        var saving by remember { mutableStateOf(false) }
        LaunchedEffect(dataSource) {
            val source = dataSource ?: return@LaunchedEffect
            draft = kagariLoadState { source.kagariLoadProfileDraft() }
        }
        return object : KagariProfileEditState {
            override val draft: UiState<KagariProfileDraft> = draft
            override val saving: Boolean = saving

            override fun save(
                after: KagariProfileDraft,
                avatar: KagariProfileImage?,
                banner: KagariProfileImage?,
                onResult: (Boolean) -> Unit,
            ) {
                val source = dataSource ?: return onResult(false)
                val before = draft.takeSuccess() ?: return onResult(false)
                scope.kagariSubmit(busy = { saving = it }, onResult = onResult) {
                    source.kagariSaveProfile(before, after, avatar, banner)
                }
            }
        }
    }
}

@Immutable
public interface KagariProfileEditState {
    public val draft: UiState<KagariProfileDraft>
    public val saving: Boolean

    public fun save(
        after: KagariProfileDraft,
        avatar: KagariProfileImage?,
        banner: KagariProfileImage?,
        onResult: (Boolean) -> Unit,
    )
}
