package dev.dimension.flare.ui.screen.profile

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Image
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Trash
import compose.icons.fontawesomeicons.solid.Xmark
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.bluesky.KagariProfileDraft
import dev.dimension.flare.data.datasource.bluesky.KagariProfileImage
import dev.dimension.flare.data.datasource.bluesky.KagariProfileLink
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.NetworkImage
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.model.toUiImage
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariProfileEditPresenter
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariProfileEditScreen(
    accountKey: MicroBlogKey,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by producePresenter(key = "KagariProfileEdit_$accountKey") {
        remember(accountKey) { KagariProfileEditPresenter(accountKey) }.invoke()
    }
    var loaded by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pronouns by remember { mutableStateOf("") }
    val links = remember { mutableStateListOf<KagariEditableLink>() }
    var avatar by remember { mutableStateOf<Bitmap?>(null) }
    var banner by remember { mutableStateOf<Bitmap?>(null) }
    var cropping by remember { mutableStateOf<Pair<Bitmap, KagariCropShape>?>(null) }
    var pickingShape by remember { mutableStateOf(KagariCropShape.Avatar) }
    var confirmDiscard by remember { mutableStateOf(false) }

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

    fun current(): KagariProfileDraft? =
        draft?.copy(
            displayName = displayName.trim(),
            description = description.trim(),
            pronouns = pronouns.trim(),
            links =
                links
                    .filter { it.url.isNotBlank() }
                    .map { KagariProfileLink(url = it.url.trim(), title = it.title.trim(), uri = it.uri, cid = it.cid) },
        )

    val changed = loaded && (current() != draft || avatar != null || banner != null)
    val linksValid = links.all { it.url.isBlank() || it.url.trim().startsWith("https://") }
    val canSave =
        loaded && changed && !state.saving && linksValid &&
            displayName.length <= KAGARI_NAME_MAX &&
            description.length <= KAGARI_DESCRIPTION_MAX &&
            pronouns.length <= KAGARI_PRONOUNS_MAX

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                kagariLoadBitmap(context, uri)?.let { cropping = it to pickingShape }
            }
        }

    fun close() {
        if (changed) confirmDiscard = true else onBack()
    }

    BackHandler(enabled = changed) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.kagari_profile_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = ::close) {
                        FAIcon(imageVector = FontAwesomeIcons.Solid.Xmark, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            val after = current() ?: return@TextButton
                            state.save(
                                after = after,
                                avatar = avatar?.let { KagariProfileImage(kagariEncodeUnder(it), "image/jpeg") },
                                banner = banner?.let { KagariProfileImage(kagariEncodeUnder(it), "image/jpeg") },
                            ) { ok ->
                                Toast
                                    .makeText(
                                        context,
                                        if (ok) R.string.kagari_profile_edit_saved else R.string.kagari_profile_edit_failed,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                if (ok) onBack()
                            }
                        },
                    ) {
                        Text(text = stringResource(R.string.kagari_profile_edit_save))
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state.draft) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.kagari_profile_edit_load_failed))
                }
            }

            is UiState.Success -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    pickingShape = KagariCropShape.Banner
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                    ) {
                        val newBanner = banner
                        if (newBanner != null) {
                            Image(
                                bitmap = newBanner.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (current.data.bannerUrl != null) {
                            NetworkImage(
                                model = current.data.bannerUrl.toUiImage(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        KagariPickBadge(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
                    }
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 16.dp)
                                .offset(y = (-36).dp)
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    pickingShape = KagariCropShape.Avatar
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                    ) {
                        val newAvatar = avatar
                        if (newAvatar != null) {
                            Image(
                                bitmap = newAvatar.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (current.data.avatarUrl != null) {
                            NetworkImage(
                                model = current.data.avatarUrl.toUiImage(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        KagariPickBadge(modifier = Modifier.align(Alignment.BottomEnd))
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-20).dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            }
        }
    }

    cropping?.let { (bitmap, shape) ->
        KagariImageCropper(
            bitmap = bitmap,
            shape = shape,
            onCancel = { cropping = null },
            onCropped = { result ->
                if (shape == KagariCropShape.Avatar) avatar = result else banner = result
                cropping = null
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(text = stringResource(R.string.kagari_profile_edit_discard_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        onBack()
                    },
                ) {
                    Text(text = stringResource(R.string.kagari_profile_edit_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun KagariPickBadge(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        FAIcon(
            imageVector = FontAwesomeIcons.Solid.Image,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun KagariCountedField(
    label: String,
    value: String,
    max: Int,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = value.length > max,
        supportingText = {
            Text(
                text = stringResource(R.string.kagari_profile_edit_remaining, max - value.length),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
