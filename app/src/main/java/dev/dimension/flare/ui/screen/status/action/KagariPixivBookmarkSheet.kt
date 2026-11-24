package dev.dimension.flare.ui.screen.status.action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.microblog.datasource.KagariPixivBookmarkDetail
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariPixivBookmarkPresenter
import dev.dimension.flare.ui.presenter.status.action.KagariPixivBookmarkState
import dev.dimension.flare.ui.screen.KagariMessageDialog
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariPixivBookmarkSheet(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariPixivBookmarkPresenter_${accountType}_$statusKey") {
        kagariPixivBookmarkPresenter(statusKey = statusKey, accountType = accountType)
    }
    var failed by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = screenHorizontalPadding)
                .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.kagari_pixiv_bookmark_title),
            style = MaterialTheme.typography.titleLarge,
        )
        when (val detail = state.detail) {
            is UiState.Success -> {
                KagariPixivBookmarkEditor(
                    detail = detail.data,
                    saving = state.saving,
                    onSave = { isPrivate, tags ->
                        state.save(isPrivate, tags) { ok ->
                            if (ok) onBack() else failed = true
                        }
                    },
                    onRemove = {
                        state.remove { ok ->
                            if (ok) onBack() else failed = true
                        }
                    },
                )
            }

            is UiState.Error -> {
                Text(
                    text = stringResource(R.string.kagari_pixiv_bookmark_load_failed),
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = state::retry) {
                    Text(stringResource(R.string.kagari_pixiv_bookmark_retry))
                }
            }

            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    if (failed) {
        KagariMessageDialog(
            message = stringResource(R.string.kagari_pixiv_bookmark_failed),
            onDismissRequest = { failed = false },
        )
    }
}

private data class KagariPixivBookmarkTagSelection(
    val name: String,
    val selected: Boolean,
)

@Composable
private fun KagariPixivBookmarkEditor(
    detail: KagariPixivBookmarkDetail,
    saving: Boolean,
    onSave: (Boolean, List<String>) -> Unit,
    onRemove: () -> Unit,
) {
    var isPrivate by remember(detail) { mutableStateOf(detail.isPrivate) }
    var input by remember(detail) { mutableStateOf("") }
    val tags =
        remember(detail) {
            mutableStateListOf<KagariPixivBookmarkTagSelection>().apply {
                addAll(detail.tags.map { KagariPixivBookmarkTagSelection(it.name, it.isRegistered) })
            }
        }

    fun addTag() {
        val name = input.trim()
        if (name.isEmpty()) return
        val index = tags.indexOfFirst { it.name == name }
        if (index >= 0) {
            tags.removeAt(index)
        }
        tags.add(0, KagariPixivBookmarkTagSelection(name, true))
        input = ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.kagari_pixiv_bookmark_private),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isPrivate,
            onCheckedChange = { isPrivate = it },
            enabled = !saving,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.kagari_pixiv_bookmark_tag_hint)) },
            singleLine = true,
            enabled = !saving,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = ::addTag,
            enabled = !saving && input.isNotBlank(),
        ) {
            Text(stringResource(R.string.kagari_pixiv_bookmark_tag_add))
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEachIndexed { index, tag ->
            FilterChip(
                selected = tag.selected,
                onClick = { tags[index] = tag.copy(selected = !tag.selected) },
                label = { Text(tag.name) },
                enabled = !saving,
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (detail.isBookmarked) {
            TextButton(
                onClick = onRemove,
                enabled = !saving,
            ) {
                Text(stringResource(R.string.kagari_pixiv_bookmark_remove))
            }
        }
        Button(
            onClick = { onSave(isPrivate, tags.filter { it.selected }.map { it.name }) },
            enabled = !saving,
        ) {
            Text(stringResource(R.string.kagari_pixiv_bookmark_save))
        }
    }
}

@Composable
private fun kagariPixivBookmarkPresenter(
    statusKey: MicroBlogKey,
    accountType: AccountType,
) = run {
    val state =
        remember(statusKey, accountType) {
            KagariPixivBookmarkPresenter(accountType = accountType, statusKey = statusKey)
        }.invoke()
    object : KagariPixivBookmarkState by state {
    }
}
