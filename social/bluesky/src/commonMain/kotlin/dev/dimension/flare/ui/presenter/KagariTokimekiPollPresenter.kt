package dev.dimension.flare.ui.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import dev.dimension.flare.data.network.bluesky.KagariTokimekiPollClient
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.KagariTokimekiPoll
import dev.dimension.flare.ui.model.UiState

public class KagariTokimekiPollPresenter(
    private val accountType: AccountType,
    private val embed: KagariEmbed.TokimekiPoll,
) : PresenterBase<UiState<KagariTokimekiPoll>>() {
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): UiState<KagariTokimekiPoll> {
        val dataSource = accountService.rememberKagariBluesky(accountType)
        return produceState<UiState<KagariTokimekiPoll>>(UiState.Loading(), dataSource) {
            val viewer = dataSource?.accountKey?.id ?: return@produceState
            value =
                runCatching { KagariTokimekiPollClient.poll(embed.did, embed.rkey, embed.url, viewer) }
                    .getOrNull()
                    ?.let { UiState.Success(it) }
                    ?: UiState.Error(IllegalStateException(embed.url))
        }.value
    }
}
