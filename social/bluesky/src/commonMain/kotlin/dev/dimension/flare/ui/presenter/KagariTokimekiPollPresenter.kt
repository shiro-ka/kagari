package dev.dimension.flare.ui.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.kagariTokimekiUnvote
import dev.dimension.flare.data.datasource.bluesky.kagariTokimekiVote
import dev.dimension.flare.data.network.bluesky.KagariTokimekiPollClient
import dev.dimension.flare.data.network.bluesky.kagariWithVote
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.KagariTokimekiPoll
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.takeSuccess
import kotlinx.coroutines.CoroutineScope

public class KagariTokimekiPollPresenter(
    private val accountType: AccountType,
    private val embed: KagariEmbed.TokimekiPoll,
) : PresenterBase<KagariTokimekiPollState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariTokimekiPollState {
        val dataSource = accountService.rememberKagariBluesky(accountType)
        val server by produceState<UiState<KagariTokimekiPoll>>(UiState.Loading(), dataSource) {
            val viewer = dataSource?.accountKey?.id ?: return@produceState
            value =
                runCatching { KagariTokimekiPollClient.poll(embed.did, embed.rkey, embed.url, viewer) }
                    .getOrNull()
                    ?.let { UiState.Success(it) }
                    ?: UiState.Error(IllegalStateException(embed.url))
        }
        val localVotes by KagariTokimekiPollClient.localVotes.collectAsState()
        var selectedIndex by remember { mutableStateOf<Int?>(null) }
        var isBusy by remember { mutableStateOf(false) }
        var lastFailure by remember { mutableStateOf<KagariTokimekiPollFailure?>(null) }
        val serverPoll = server.takeSuccess()
        val voteKey =
            dataSource?.let { source ->
                serverPoll?.let { KagariTokimekiPollClient.localVoteKey(source.accountKey.id, it.uri) }
            }
        val localVote = voteKey?.let { localVotes[it] }
        val currentPoll = serverPoll?.let { poll -> localVote?.let { poll.kagariWithVote(it) } ?: poll }
        val votable =
            dataSource != null && currentPoll != null && !currentPoll.isEnded && !currentPoll.isAuthor && currentPoll.myVote == null
        val cancellable =
            dataSource != null && currentPoll != null && !currentPoll.isEnded && currentPoll.myVoteUri != null && !isBusy
        return object : KagariTokimekiPollState {
            override val poll: KagariTokimekiPoll? = currentPoll
            override val selected: Int? = selectedIndex.takeIf { votable }
            override val busy: Boolean = isBusy
            override val canVote: Boolean = votable
            override val canCancel: Boolean = cancellable
            override val failure: KagariTokimekiPollFailure? = lastFailure

            override fun select(index: Int) {
                if (votable && !isBusy && currentPoll != null && index in currentPoll.options.indices) {
                    selectedIndex = index
                }
            }

            override fun vote() {
                val source = dataSource ?: return
                val target = currentPoll ?: return
                val key = voteKey ?: return
                val index = selectedIndex ?: return
                if (!votable || isBusy || index !in target.options.indices) {
                    return
                }
                val previous = localVote
                lastFailure = null
                KagariTokimekiPollClient.setLocalVote(key, KagariTokimekiPollClient.LocalVote(index = index, uri = null))
                scope.kagariLaunch(
                    busy = { isBusy = it },
                    onDone = { result ->
                        result.fold(
                            onSuccess = { uri ->
                                KagariTokimekiPollClient.setLocalVote(key, KagariTokimekiPollClient.LocalVote(index = index, uri = uri))
                                selectedIndex = null
                            },
                            onFailure = {
                                KagariTokimekiPollClient.setLocalVote(key, previous)
                                lastFailure = KagariTokimekiPollFailure.Vote
                            },
                        )
                    },
                ) {
                    source.kagariService().kagariTokimekiVote(source.accountKey, target.uri, target.cid, target.options[index].index)
                }
            }

            override fun cancel() {
                val source = dataSource ?: return
                val target = currentPoll ?: return
                val key = voteKey ?: return
                val voteUri = target.myVoteUri ?: return
                if (!cancellable) {
                    return
                }
                val previous = localVote ?: KagariTokimekiPollClient.LocalVote(index = target.myVote, uri = voteUri)
                lastFailure = null
                KagariTokimekiPollClient.setLocalVote(key, KagariTokimekiPollClient.LocalVote(index = null, uri = null))
                scope.kagariLaunch(
                    busy = { isBusy = it },
                    onDone = { result ->
                        result.fold(
                            onSuccess = { selectedIndex = null },
                            onFailure = {
                                KagariTokimekiPollClient.setLocalVote(key, previous)
                                lastFailure = KagariTokimekiPollFailure.Cancel
                            },
                        )
                    },
                ) {
                    source.kagariService().kagariTokimekiUnvote(source.accountKey, voteUri)
                }
            }
        }
    }
}

public enum class KagariTokimekiPollFailure {
    Vote,
    Cancel,
}

@Immutable
public interface KagariTokimekiPollState {
    public val poll: KagariTokimekiPoll?
    public val selected: Int?
    public val busy: Boolean
    public val canVote: Boolean
    public val canCancel: Boolean
    public val failure: KagariTokimekiPollFailure?

    public fun select(index: Int)

    public fun vote()

    public fun cancel()
}
