package dev.dimension.flare.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import dev.dimension.flare.ui.model.UiState

@Composable
internal fun changeLogPresenter(): ChangeLogState = KagariNoChangeLog

private object KagariNoChangeLog : ChangeLogState {
    override val shouldShowChangeLog: UiState<Boolean> = UiState.Success(false)
    override val changeLog: AnnotatedString? = null

    override fun dismissChangeLog() = Unit
}

@Immutable
interface ChangeLogState {
    val shouldShowChangeLog: UiState<Boolean>
    val changeLog: AnnotatedString?

    fun dismissChangeLog()
}
