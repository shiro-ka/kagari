package dev.dimension.flare.ui.presenter.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import dev.dimension.flare.data.datasource.microblog.KagariMadoSource
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.KagariMadoProfile
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.PresenterBase

public class KagariMadoPresenter(
    private val source: KagariMadoSource,
    private val userKey: MicroBlogKey,
) : PresenterBase<UiState<KagariMadoProfile>>() {
    @Composable
    override fun body(): UiState<KagariMadoProfile> {
        val state by produceState<UiState<KagariMadoProfile>>(UiState.Loading(), source, userKey) {
            value =
                runCatching { source.kagariMadoProfile(userKey) }
                    .fold(
                        onSuccess = { UiState.Success(it) },
                        onFailure = { UiState.Error(it) },
                    )
        }
        return state
    }
}
