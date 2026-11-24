package dev.dimension.flare.ui.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import dev.dimension.flare.data.network.misskey.KagariMisskeyUrlPreviewClient
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.UiCard
import dev.dimension.flare.ui.model.UiState

public class KagariMisskeyUrlPreviewPresenter(
    private val accountType: AccountType,
    private val embed: KagariEmbed.MisskeyUrlPreview,
) : PresenterBase<UiState<UiCard>>() {
    @Composable
    override fun body(): UiState<UiCard> =
        produceState<UiState<UiCard>>(UiState.Loading(), accountType, embed) {
            val accountKey = (accountType as? AccountType.Specific)?.accountKey
            value =
                accountKey
                    ?.let { KagariMisskeyUrlPreviewClient.preview(it, embed.url) }
                    ?.let { UiState.Success(it) }
                    ?: UiState.Error(IllegalStateException(embed.url))
        }.value
}
