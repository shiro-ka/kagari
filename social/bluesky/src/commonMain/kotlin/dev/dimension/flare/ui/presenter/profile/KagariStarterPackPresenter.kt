package dev.dimension.flare.ui.presenter.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.KagariStarterPackDetail
import dev.dimension.flare.data.datasource.bluesky.kagariFollow
import dev.dimension.flare.data.datasource.bluesky.kagariLoadStarterPack
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.kagariLoadState
import dev.dimension.flare.ui.presenter.rememberKagariBluesky
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

public class KagariStarterPackPresenter(
    private val accountKey: MicroBlogKey,
    private val uri: String,
) : PresenterBase<KagariStarterPackState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariStarterPackState {
        val bluesky = accountService.rememberKagariBluesky(AccountType.Specific(accountKey))
        var detail by remember { mutableStateOf<UiState<KagariStarterPackDetail>>(UiState.Loading()) }
        var followed by remember { mutableStateOf(emptySet<MicroBlogKey>()) }
        var following by remember { mutableStateOf(emptySet<MicroBlogKey>()) }
        LaunchedEffect(bluesky, uri) {
            val source = bluesky ?: return@LaunchedEffect
            detail = kagariLoadState { source.kagariService().kagariLoadStarterPack(accountKey, uri) }
        }

        fun followUsers(
            users: List<MicroBlogKey>,
            onResult: (Int) -> Unit,
        ) {
            val source = bluesky ?: return onResult(users.size)
            following = following + users
            scope.launch {
                val service = source.kagariService()
                var failed = 0
                users.forEach { user ->
                    val ok = runCatching { service.kagariFollow(accountKey, user) }.isSuccess
                    withContext(Dispatchers.Main) {
                        following = following - user
                        if (ok) followed = followed + user else failed++
                    }
                }
                withContext(Dispatchers.Main) {
                    onResult(failed)
                }
            }
        }

        val notFollowing =
            detail
                .takeSuccess()
                ?.members
                ?.filter { !it.following && it.profile.key !in followed }
                ?.map { it.profile.key }
                .orEmpty()

        return object : KagariStarterPackState {
            override val detail: UiState<KagariStarterPackDetail> = detail
            override val followed: Set<MicroBlogKey> = followed
            override val following: Set<MicroBlogKey> = following
            override val notFollowingCount: Int = notFollowing.size

            override fun follow(
                userKey: MicroBlogKey,
                onResult: (Int) -> Unit,
            ) {
                followUsers(listOf(userKey), onResult)
            }

            override fun followAll(onResult: (Int) -> Unit) {
                followUsers(notFollowing, onResult)
            }
        }
    }
}

@Immutable
public interface KagariStarterPackState {
    public val detail: UiState<KagariStarterPackDetail>
    public val followed: Set<MicroBlogKey>
    public val following: Set<MicroBlogKey>
    public val notFollowingCount: Int

    public fun follow(
        userKey: MicroBlogKey,
        onResult: (Int) -> Unit,
    )

    public fun followAll(onResult: (Int) -> Unit)
}
