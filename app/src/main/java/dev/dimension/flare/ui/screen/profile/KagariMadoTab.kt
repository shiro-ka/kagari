package dev.dimension.flare.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.ui.component.status.KagariMadoBoxCard
import dev.dimension.flare.ui.component.status.KagariMadoVoiceCard
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariMadoPresenter
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariMadoTab(
    id: String,
    presenter: KagariMadoPresenter,
    bottomPadding: Dp,
) {
    val state by producePresenter(key = "KagariMado_$id") {
        presenter.invoke()
    }
    when (val current = state) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {}

        is UiState.Success -> {
            val profile = current.data
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp + bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "kagari_mado_boxes_header") {
                    KagariMadoHeader(stringResource(R.string.kagari_mado_boxes))
                }
                items(profile.boxes, key = { "box_${it.uri}" }) { box ->
                    KagariMadoBoxCard(box = box)
                }
                item(key = "kagari_mado_voices_header") {
                    KagariMadoHeader(stringResource(R.string.kagari_mado_answered_voices))
                }
                if (profile.voices.isEmpty()) {
                    item(key = "kagari_mado_voices_empty") {
                        Text(
                            text = stringResource(R.string.kagari_mado_no_answered_voices),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        )
                    }
                } else {
                    items(profile.voices, key = { "voice_${it.uri}" }) { voice ->
                        KagariMadoVoiceCard(
                            voice = voice,
                            footer = voice.boxTitle,
                            date = voice.answeredAt,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KagariMadoHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
    )
}
