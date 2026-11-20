package dev.dimension.flare.ui.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.atproto.identity.ResolveHandleQueryParams
import dev.dimension.flare.data.network.bluesky.KagariMadoClient
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.KagariMadoVoice
import dev.dimension.flare.ui.model.UiState
import sh.christian.ozone.api.Handle

public class KagariMadoVoicePresenter(
    private val accountType: AccountType,
    private val embed: KagariEmbed.MadoVoice,
) : PresenterBase<UiState<KagariMadoVoice>>() {
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): UiState<KagariMadoVoice> {
        val dataSource = accountService.rememberKagariBluesky(accountType)
        return produceState<UiState<KagariMadoVoice>>(UiState.Loading(), dataSource) {
            val did =
                embed.ownerDid
                    ?: embed.handle.takeIf { it.startsWith("did:") }
                    ?: dataSource?.let { source ->
                        runCatching {
                            source
                                .kagariService()
                                .resolveHandle(ResolveHandleQueryParams(Handle(embed.handle)))
                                .requireResponse()
                                .did
                                .did
                        }.getOrNull()
                    }
            if (did == null) {
                if (dataSource != null) {
                    value = UiState.Error(IllegalStateException(embed.url))
                }
                return@produceState
            }
            value =
                runCatching { KagariMadoClient.voice(did, embed.handle, embed.rkey) }
                    .getOrNull()
                    ?.let { UiState.Success(it) }
                    ?: UiState.Error(IllegalStateException(embed.url))
        }.value
    }
}
