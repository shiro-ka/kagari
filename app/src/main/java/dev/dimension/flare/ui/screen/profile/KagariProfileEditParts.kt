package dev.dimension.flare.ui.screen.profile

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Image
import compose.icons.fontawesomeicons.solid.Trash
import compose.icons.fontawesomeicons.solid.Xmark
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.microblog.KagariProfileDraftImages
import dev.dimension.flare.data.datasource.microblog.KagariProfileImage
import dev.dimension.flare.data.datasource.microblog.KagariProfileImageChanges
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.NetworkImage
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.model.toUiImage
import dev.dimension.flare.ui.presenter.profile.KagariProfileEditState
import dev.dimension.flare.ui.screen.KagariConfirmDialog
import dev.dimension.flare.ui.screen.KagariLoadingBox
import dev.dimension.flare.ui.screen.KagariMessageBox
import dev.dimension.flare.ui.screen.kagariToast

@Stable
internal class KagariProfileImagesState {
    var avatar by mutableStateOf<Bitmap?>(null)
    var banner by mutableStateOf<Bitmap?>(null)
    var bannerRemoved by mutableStateOf(false)

    val changed: Boolean get() = avatar != null || banner != null || bannerRemoved

    fun changes(): KagariProfileImageChanges =
        KagariProfileImageChanges(
            avatar = avatar?.let { KagariProfileImage(kagariEncodeUnder(it), "image/jpeg") },
            banner = banner?.let { KagariProfileImage(kagariEncodeUnder(it), "image/jpeg") },
            removeBanner = bannerRemoved && banner == null,
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <D : KagariProfileDraftImages> KagariProfileEditScaffold(
    state: KagariProfileEditState<D>,
    edited: D?,
    fieldsValid: Boolean,
    onBack: () -> Unit,
    fields: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val images = remember { KagariProfileImagesState() }
    var cropping by remember { mutableStateOf<Pair<Bitmap, KagariCropShape>?>(null) }
    var pickingShape by remember { mutableStateOf(KagariCropShape.Avatar) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val loaded = state.draft.takeSuccess()
    val changed = loaded != null && edited != null && (edited != loaded || images.changed)
    val canSave = changed && fieldsValid && !state.saving

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                kagariLoadBitmap(context, uri)?.let { cropping = it to pickingShape }
            }
        }

    fun pick(shape: KagariCropShape) {
        pickingShape = shape
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                            val after = edited ?: return@TextButton
                            state.save(after = after, images = images.changes()) { ok ->
                                kagariToast(
                                    context,
                                    if (ok) R.string.kagari_profile_edit_saved else R.string.kagari_profile_edit_failed,
                                )
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
                KagariLoadingBox(padding = padding)
            }

            is UiState.Error -> {
                KagariMessageBox(message = stringResource(R.string.kagari_profile_edit_load_failed), padding = padding)
            }

            is UiState.Success -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState()),
                ) {
                    val bannerUrl = current.data.bannerUrl?.takeUnless { images.bannerRemoved }
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(KagariCropShape.Banner.aspect)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { pick(KagariCropShape.Banner) },
                    ) {
                        KagariProfileImagePreview(bitmap = images.banner, url = bannerUrl)
                        if (images.banner != null || bannerUrl != null) {
                            KagariPickBadge(
                                icon = FontAwesomeIcons.Solid.Trash,
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clickable {
                                            images.banner = null
                                            images.bannerRemoved = current.data.bannerUrl != null
                                        },
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
                                .clickable { pick(KagariCropShape.Avatar) },
                    ) {
                        KagariProfileImagePreview(bitmap = images.avatar, url = current.data.avatarUrl)
                        KagariPickBadge(modifier = Modifier.align(Alignment.BottomEnd))
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-20).dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = fields,
                    )
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
                when (shape) {
                    KagariCropShape.Avatar -> {
                        images.avatar = result
                    }

                    KagariCropShape.Banner -> {
                        images.banner = result
                        images.bannerRemoved = false
                    }
                }
                cropping = null
            },
        )
    }

    if (confirmDiscard) {
        KagariConfirmDialog(
            title = stringResource(R.string.kagari_profile_edit_discard_title),
            confirmText = stringResource(R.string.kagari_profile_edit_discard),
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismissRequest = { confirmDiscard = false },
        )
    }
}

@Composable
private fun KagariProfileImagePreview(
    bitmap: Bitmap?,
    url: String?,
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else if (url != null) {
        NetworkImage(
            model = url.toUiImage(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun KagariPickBadge(
    modifier: Modifier = Modifier,
    icon: ImageVector = FontAwesomeIcons.Solid.Image,
) {
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        FAIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
internal fun KagariCountedField(
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
