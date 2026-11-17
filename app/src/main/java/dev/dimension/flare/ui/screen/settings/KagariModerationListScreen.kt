package dev.dimension.flare.ui.screen.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.bluesky.kagariBlockedAccounts
import dev.dimension.flare.data.datasource.bluesky.kagariMutedAccounts
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariUserListPresenter
import dev.dimension.flare.ui.presenter.profile.UnblockUserPresenter
import dev.dimension.flare.ui.presenter.profile.UnmuteUserPresenter
import dev.dimension.flare.ui.presenter.profile.UserListPresenter
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.toUri
import dev.dimension.flare.ui.screen.profile.UserListScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import moe.tlaster.precompose.molecule.producePresenter

@Serializable
internal enum class KagariModerationListKind {
    Muted,
    Blocked,
}

@Composable
internal fun KagariModerationListScreen(
    accountKey: MicroBlogKey,
    kind: KagariModerationListKind,
    onBack: () -> Unit,
) {
    val accountType = AccountType.Specific(accountKey)
    val state by producePresenter(key = "KagariModerationList_${accountKey}_$kind") {
        val scope = rememberCoroutineScope()
        val state =
            remember(accountKey, kind) {
                KagariUserListPresenter(accountType) { service ->
                    when (kind) {
                        KagariModerationListKind.Muted -> service.kagariMutedAccounts()
                        KagariModerationListKind.Blocked -> service.kagariBlockedAccounts()
                    }
                }
            }.invoke()
        object : UserListPresenter.State by state {
            fun refresh() {
                scope.launch {
                    state.refreshSuspend()
                }
            }
        }
    }
    val uriHandler = LocalUriHandler.current
    var pending by remember { mutableStateOf<MicroBlogKey?>(null) }
    var released by remember { mutableStateOf(emptySet<MicroBlogKey>()) }
    UserListScreen(
        data = state.listState,
        emptyMessage =
            stringResource(
                when (kind) {
                    KagariModerationListKind.Muted -> R.string.kagari_moderation_muted_empty
                    KagariModerationListKind.Blocked -> R.string.kagari_moderation_blocked_empty
                },
            ),
        title = {
            Text(
                text =
                    stringResource(
                        when (kind) {
                            KagariModerationListKind.Muted -> R.string.kagari_moderation_muted_title
                            KagariModerationListKind.Blocked -> R.string.kagari_moderation_blocked_title
                        },
                    ),
            )
        },
        onBack = onBack,
        isRefreshing = state.listState.isRefreshing,
        refresh = state::refresh,
        onUserClick = { userKey ->
            uriHandler.openUri(
                DeeplinkRoute.Profile
                    .User(
                        accountType = accountType,
                        userKey = userKey,
                    ).toUri(),
            )
        },
        trailingContent = { profile ->
            if (profile.key in released) {
                Text(text = stringResource(R.string.kagari_moderation_released))
            } else {
                OutlinedButton(onClick = { pending = profile.key }) {
                    Text(text = stringResource(R.string.kagari_moderation_release))
                }
            }
        },
    )
    pending?.let { userKey ->
        KagariReleaseDialog(
            accountType = accountType,
            userKey = userKey,
            kind = kind,
            onConfirmed = {
                released = released + userKey
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun KagariReleaseDialog(
    accountType: AccountType,
    userKey: MicroBlogKey,
    kind: KagariModerationListKind,
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirm: () -> Unit =
        when (kind) {
            KagariModerationListKind.Muted -> {
                val state by producePresenter("kagari_unmute_${accountType}_$userKey") {
                    remember(userKey) { UnmuteUserPresenter(accountType, userKey) }.body()
                }
                state::confirm
            }

            KagariModerationListKind.Blocked -> {
                val state by producePresenter("kagari_unblock_${accountType}_$userKey") {
                    remember(userKey) { UnblockUserPresenter(accountType, userKey) }.body()
                }
                state::confirm
            }
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    stringResource(
                        when (kind) {
                            KagariModerationListKind.Muted -> R.string.unmute_user_title
                            KagariModerationListKind.Blocked -> R.string.unblock_user_title
                        },
                    ),
            )
        },
        text = {
            Text(
                text =
                    stringResource(
                        when (kind) {
                            KagariModerationListKind.Muted -> R.string.unmute_user_description
                            KagariModerationListKind.Blocked -> R.string.unblock_user_description
                        },
                    ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    confirm()
                    onConfirmed()
                },
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
    )
}
