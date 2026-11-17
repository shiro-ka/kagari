package dev.dimension.flare.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.AvatarComponent
import dev.dimension.flare.ui.component.KagariVerifiedCheck
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariVerificationPresenter
import dev.dimension.flare.ui.presenter.profile.KagariVerifier
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.toUri
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariVerificationDialog(
    accountType: AccountType,
    userKey: MicroBlogKey,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariVerificationPresenter_${accountType}_$userKey") {
        remember(accountType, userKey) {
            KagariVerificationPresenter(accountType = accountType, userKey = userKey)
        }.invoke()
    }
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onBack,
        confirmButton = {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.kagari_verification_close))
            }
        },
        title = {
            Text(text = stringResource(R.string.kagari_verification_title))
        },
        text = {
            when (val verifiers = state.verifiers) {
                is UiState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Text(text = stringResource(R.string.kagari_verification_message_simple))
                }

                is UiState.Success -> {
                    if (verifiers.data.isEmpty()) {
                        Text(text = stringResource(R.string.kagari_verification_message_simple))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = stringResource(R.string.kagari_verification_message))
                            verifiers.data.forEach { verifier ->
                                KagariVerifierRow(
                                    verifier = verifier,
                                    onClick = {
                                        onBack()
                                        uriHandler.openUri(
                                            DeeplinkRoute.Profile
                                                .User(
                                                    accountType = accountType,
                                                    userKey = verifier.profile.key,
                                                ).toUri(),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun KagariVerifierRow(
    verifier: KagariVerifier,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarComponent(data = verifier.profile.avatar, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = verifier.profile.name.raw,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                KagariVerifiedCheck(user = verifier.profile, size = 14.dp)
            }
            Text(
                text = stringResource(R.string.kagari_verification_verified_on, verifier.createdAt.absolute),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
