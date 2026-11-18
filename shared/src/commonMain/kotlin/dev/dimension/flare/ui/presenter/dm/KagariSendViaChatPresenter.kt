package dev.dimension.flare.ui.presenter.dm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.paging.compose.collectAsLazyPagingItems
import dev.dimension.flare.common.PagingState
import dev.dimension.flare.common.toPagingState
import dev.dimension.flare.data.datasource.microblog.capabilities
import dev.dimension.flare.data.repository.AccountRepository
import dev.dimension.flare.data.repository.accountServiceProvider
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiDMRoom
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.map
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.coroutines.launch

public class KagariSendViaChatPresenter(
    private val accountKey: MicroBlogKey,
    private val uri: String,
    private val cid: String,
) : PresenterBase<KagariSendViaChatState>() {
    private val accountRepository: AccountRepository by koinInject()

    @Composable
    override fun body(): KagariSendViaChatState {
        val scope = rememberCoroutineScope()
        val serviceState =
            accountServiceProvider(accountType = AccountType.Specific(accountKey), repository = accountRepository)
        val directMessageState =
            remember(serviceState) {
                serviceState.map { requireNotNull(it.capabilities.directMessage) }
            }
        val directMessage = (directMessageState as? UiState.Success)?.data
        var sending by remember { mutableStateOf(false) }
        val rooms =
            directMessageState
                .map { source ->
                    remember(source) {
                        source.directMessageList(scope = scope)
                    }.collectAsLazyPagingItems()
                }.toPagingState()
        return object : KagariSendViaChatState {
            override val rooms: PagingState<UiDMRoom> = rooms
            override val sending: Boolean = sending

            override fun send(
                roomKey: MicroBlogKey,
                text: String,
                onResult: (Boolean) -> Unit,
            ) {
                val handler = directMessage?.directMessageHandler ?: return onResult(false)
                sending = true
                scope.launch {
                    val ok = handler.kagariSendPost(roomKey, text, uri, cid)
                    sending = false
                    onResult(ok)
                }
            }
        }
    }
}

@Immutable
public interface KagariSendViaChatState {
    public val rooms: PagingState<UiDMRoom>
    public val sending: Boolean

    public fun send(
        roomKey: MicroBlogKey,
        text: String,
        onResult: (Boolean) -> Unit,
    )
}
