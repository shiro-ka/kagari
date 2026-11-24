package dev.dimension.flare.ui.component.status

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.UiCard
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.KagariMisskeyUrlPreviewPresenter
import dev.dimension.flare.ui.presenter.invoke
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun kagariRememberMisskeyUrlCard(
    accountType: AccountType,
    embed: KagariEmbed?,
): UiCard? {
    val preview = embed as? KagariEmbed.MisskeyUrlPreview ?: return null
    val state by producePresenter(key = "kagari_misskey_url_${accountType}_${preview.url}") {
        remember(accountType, preview) { KagariMisskeyUrlPreviewPresenter(accountType, preview) }.invoke()
    }
    return state.takeSuccess()
}
