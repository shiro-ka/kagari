package dev.dimension.flare.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.AvatarComponent
import dev.dimension.flare.ui.component.BackButton
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariStarterPackPresenter
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.KagariFeedRoute
import dev.dimension.flare.ui.route.toUri
import moe.tlaster.precompose.molecule.producePresenter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariStarterPackScreen(
    accountKey: MicroBlogKey,
    uri: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val state by producePresenter(key = "KagariStarterPack_${accountKey}_$uri") {
        remember(accountKey, uri) { KagariStarterPackPresenter(accountKey, uri) }.invoke()
    }
    var confirmFollowAll by remember { mutableStateOf(false) }
    val failedText = stringResource(R.string.kagari_starter_pack_follow_failed)
    val onFollowResult: (Int) -> Unit = { failed ->
        if (failed > 0) Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.kagari_starter_pack_title)) },
                navigationIcon = { BackButton(onBack = onBack) },
            )
        },
    ) { padding ->
        when (val detail = state.detail) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.kagari_starter_pack_failed))
                }
            }

            is UiState.Success -> {
                val pack = detail.data
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(text = pack.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = stringResource(R.string.kagari_starter_pack_by, pack.creator.handle.display),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                    Modifier.clickable {
                                        uriHandler.openUri(
                                            DeeplinkRoute.Profile
                                                .User(
                                                    accountType = AccountType.Specific(accountKey),
                                                    userKey = pack.creator.key,
                                                ).toUri(),
                                        )
                                    },
                            )
                            pack.description?.takeIf { it.isNotBlank() }?.let {
                                Text(text = it)
                            }
                            Button(
                                enabled = state.notFollowingCount > 0 && state.following.isEmpty(),
                                onClick = { confirmFollowAll = true },
                            ) {
                                Text(text = stringResource(R.string.kagari_starter_pack_follow_all))
                            }
                        }
                        HorizontalDivider()
                        Text(
                            text = stringResource(R.string.kagari_starter_pack_members, pack.members.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(pack.members, key = { it.profile.key.toString() }) { member ->
                        val profile = member.profile
                        val followed = member.following || profile.key in state.followed
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        uriHandler.openUri(
                                            DeeplinkRoute.Profile
                                                .User(
                                                    accountType = AccountType.Specific(accountKey),
                                                    userKey = profile.key,
                                                ).toUri(),
                                        )
                                    }.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvatarComponent(data = profile.avatar)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name.innerText,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = profile.handle.display,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (followed) {
                                OutlinedButton(onClick = {}, enabled = false) {
                                    Text(text = stringResource(R.string.kagari_starter_pack_following))
                                }
                            } else {
                                OutlinedButton(
                                    enabled = profile.key !in state.following,
                                    onClick = { state.follow(profile.key, onFollowResult) },
                                ) {
                                    Text(text = stringResource(R.string.kagari_starter_pack_follow))
                                }
                            }
                        }
                    }
                    if (pack.feeds.isNotEmpty()) {
                        item {
                            HorizontalDivider()
                            Text(
                                text = stringResource(R.string.kagari_starter_pack_feeds, pack.feeds.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(pack.feeds, key = { it.id }) { feed ->
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            uriHandler.openUri(KagariFeedRoute(accountKey = accountKey, uri = feed.id).toUri())
                                        }.padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(text = feed.title, style = MaterialTheme.typography.titleSmall)
                                feed.creator?.let {
                                    Text(
                                        text = it.handle.display,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmFollowAll) {
        AlertDialog(
            onDismissRequest = { confirmFollowAll = false },
            text = { Text(text = stringResource(R.string.kagari_starter_pack_follow_all_confirm, state.notFollowingCount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmFollowAll = false
                        state.followAll(onFollowResult)
                    },
                ) {
                    Text(text = stringResource(R.string.kagari_starter_pack_follow_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFollowAll = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
