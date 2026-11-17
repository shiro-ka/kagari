package dev.dimension.flare.ui.presenter.profile

import dev.dimension.flare.ui.model.UiRelation
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.takeSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun kagariProfileSourceActive(relation: Flow<UiState<UiRelation>>): Flow<Boolean> =
    relation
        .map { it.takeSuccess()?.blocking != true }
        .distinctUntilChanged()
