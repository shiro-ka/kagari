package dev.dimension.flare.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.dimension.flare.R
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariTwitterProfileEditPresenter
import moe.tlaster.precompose.molecule.producePresenter

private const val KAGARI_TWITTER_NAME_MAX = 50
private const val KAGARI_TWITTER_DESCRIPTION_MAX = 160
private const val KAGARI_TWITTER_LOCATION_MAX = 30
private const val KAGARI_TWITTER_URL_MAX = 100

@Composable
internal fun KagariTwitterProfileEditScreen(
    accountKey: MicroBlogKey,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariTwitterProfileEdit_$accountKey") {
        remember(accountKey) { KagariTwitterProfileEditPresenter(accountKey) }.invoke()
    }
    var loaded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    val draft = state.draft.takeSuccess()
    LaunchedEffect(draft) {
        if (draft != null && !loaded) {
            name = draft.name
            description = draft.description
            location = draft.location
            url = draft.url
            loaded = true
        }
    }

    val edited =
        draft?.takeIf { loaded }?.copy(
            name = name.trim(),
            description = description.trim(),
            location = location.trim(),
            url = url.trim(),
        )

    KagariProfileEditScaffold(
        state = state,
        edited = edited,
        fieldsValid =
            name.isNotBlank() &&
                name.length <= KAGARI_TWITTER_NAME_MAX &&
                description.length <= KAGARI_TWITTER_DESCRIPTION_MAX &&
                location.length <= KAGARI_TWITTER_LOCATION_MAX &&
                url.length <= KAGARI_TWITTER_URL_MAX,
        onBack = onBack,
    ) {
        KagariCountedField(
            label = stringResource(R.string.kagari_profile_edit_name),
            value = name,
            max = KAGARI_TWITTER_NAME_MAX,
            onValueChange = { name = it },
        )
        KagariCountedField(
            label = stringResource(R.string.kagari_profile_edit_description),
            value = description,
            max = KAGARI_TWITTER_DESCRIPTION_MAX,
            onValueChange = { description = it },
            singleLine = false,
        )
        KagariCountedField(
            label = stringResource(R.string.kagari_profile_edit_location),
            value = location,
            max = KAGARI_TWITTER_LOCATION_MAX,
            onValueChange = { location = it },
        )
        KagariCountedField(
            label = stringResource(R.string.kagari_profile_edit_website),
            value = url,
            max = KAGARI_TWITTER_URL_MAX,
            onValueChange = { url = it },
        )
    }
}
