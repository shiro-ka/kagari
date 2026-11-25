package dev.dimension.flare.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Trash
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.bluesky.KagariProfileLink
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariBlueskyProfileEditPresenter
import moe.tlaster.precompose.molecule.producePresenter

private const val KAGARI_NAME_MAX = 64
private const val KAGARI_DESCRIPTION_MAX = 256
private const val KAGARI_PRONOUNS_MAX = 20
private const val KAGARI_LINK_TITLE_MAX = 40
private const val KAGARI_LINKS_MAX = 10

private data class KagariEditableLink(
    val url: String,
    val title: String,
    val uri: String?,
    val cid: String?,
)

@Composable
internal fun KagariBlueskyProfileEditScreen(
    accountKey: MicroBlogKey,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariBlueskyProfileEdit_$accountKey") {
        remember(accountKey) { KagariBlueskyProfileEditPresenter(accountKey) }.invoke()
    }
    var loaded by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pronouns by remember { mutableStateOf("") }
    val links = remember { mutableStateListOf<KagariEditableLink>() }

    val draft = state.draft.takeSuccess()
    LaunchedEffect(draft) {
        if (draft != null && !loaded) {
            displayName = draft.displayName
            description = draft.description
            pronouns = draft.pronouns
            links.clear()
            links.addAll(draft.links.map { KagariEditableLink(it.url, it.title, it.uri, it.cid) })
            loaded = true
        }
    }

    val edited =
        draft?.takeIf { loaded }?.copy(
            displayName = displayName.trim(),
            description = description.trim(),
            pronouns = pronouns.trim(),
            links =
                links
                    .filter { it.url.isNotBlank() }
                    .map { KagariProfileLink(url = it.url.trim(), title = it.title.trim(), uri = it.uri, cid = it.cid) },
        )
    val linksValid = links.all { it.url.isBlank() || it.url.trim().startsWith("https://") }

    KagariProfileEditScaffold(
        state = state,
        edited = edited,
        fieldsValid =
            linksValid &&
                displayName.length <= KAGARI_NAME_MAX &&
                description.length <= KAGARI_DESCRIPTION_MAX &&
                pronouns.length <= KAGARI_PRONOUNS_MAX,
        onBack = onBack,
    ) {
        KagariCountedField(
            label = stringResource(R.string.kagari_profile_edit_name),
            value = displayName,
            max = KAGARI_NAME_MAX,
            onValueChange = { displayName = it },
        )
        KagariCountedField(
            label = stringResource(R.string.kagari_profile_edit_description),
            value = description,
            max = KAGARI_DESCRIPTION_MAX,
            onValueChange = { description = it },
            singleLine = false,
        )
        KagariCountedField(
            label = stringResource(R.string.kagari_profile_edit_pronouns),
            value = pronouns,
            max = KAGARI_PRONOUNS_MAX,
            onValueChange = { pronouns = it },
        )
        Text(
            text = stringResource(R.string.kagari_profile_edit_links),
            style = MaterialTheme.typography.titleSmall,
        )
        links.forEachIndexed { index, link ->
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = link.url,
                        onValueChange = { links[index] = link.copy(url = it) },
                        label = { Text(text = stringResource(R.string.kagari_profile_edit_link_url)) },
                        isError = link.url.isNotBlank() && !link.url.trim().startsWith("https://"),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = link.title,
                        onValueChange = { if (it.length <= KAGARI_LINK_TITLE_MAX) links[index] = link.copy(title = it) },
                        label = { Text(text = stringResource(R.string.kagari_profile_edit_link_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconButton(onClick = { links.removeAt(index) }) {
                    FAIcon(imageVector = FontAwesomeIcons.Solid.Trash, contentDescription = null)
                }
            }
        }
        if (links.size < KAGARI_LINKS_MAX) {
            TextButton(onClick = { links.add(KagariEditableLink("https://", "", null, null)) }) {
                FAIcon(imageVector = FontAwesomeIcons.Solid.Plus, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(text = stringResource(R.string.kagari_profile_edit_add_link), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
