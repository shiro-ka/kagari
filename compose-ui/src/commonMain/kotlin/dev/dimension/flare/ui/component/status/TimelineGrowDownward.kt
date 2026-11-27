package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent

internal val LocalReversedTimelineState =
    compositionLocalOf<LazyStaggeredGridState?> { null }

private class ItemExtent {
    var top = 0f
    var height = -1
}

@Composable
internal fun Modifier.growDownwardInReversedTimeline(): Modifier {
    val state = LocalReversedTimelineState.current ?: return this
    val extent = remember { ItemExtent() }
    return onGloballyPositioned { coordinates ->
        val top = coordinates.positionInParent().y
        val height = coordinates.size.height
        val previousTop = extent.top
        val previousHeight = extent.height
        extent.top = top
        extent.height = height
        val delta = height - previousHeight
        if (previousHeight < 0 || delta == 0 || previousTop < 0f || state.isScrollInProgress) return@onGloballyPositioned
        val consumed = state.dispatchRawDelta(delta.toFloat())
        extent.top = top + consumed
    }
}
