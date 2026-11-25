package dev.dimension.flare.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    pullFromBottom: Boolean = false,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()
    // workaround for https://issuetracker.google.com/issues/248274004
    SideEffect {
        if (!isRefreshing && refreshState.distanceFraction == 1f) {
            scope.launch {
                refreshState.animateToHidden()
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { refreshState.distanceFraction }
            .map { it >= 1f }
            .distinctUntilChanged()
            .drop(1) // drop initial value
            .collect {
                hapticFeedback.performHapticFeedback(
                    HapticFeedbackType.GestureThresholdActivate,
                )
            }
    }
    if (pullFromBottom) {
        val thresholdPx = with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
        val connection = remember(refreshState, scope) { BottomPullToRefreshConnection(refreshState, scope) }
        SideEffect {
            connection.isRefreshing = isRefreshing
            connection.onRefresh = onRefresh
            connection.thresholdPx = thresholdPx
        }
        LaunchedEffect(connection, isRefreshing) {
            if (isRefreshing) {
                refreshState.animateToThreshold()
            } else {
                refreshState.animateToHidden()
            }
            connection.reset()
        }
        Box(
            modifier = modifier.nestedScroll(connection),
            contentAlignment = contentAlignment,
        ) {
            content()
            PullToRefreshDefaults.LoadingIndicator(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(indicatorPadding)
                        .graphicsLayer { scaleY = -1f },
                isRefreshing = isRefreshing,
                state = refreshState,
            )
        }
        return
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        content = content,
        state = refreshState,
        contentAlignment = contentAlignment,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(indicatorPadding),
                isRefreshing = isRefreshing,
                state = refreshState,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
internal class BottomPullToRefreshConnection(
    private val state: PullToRefreshState,
    private val scope: CoroutineScope,
) : NestedScrollConnection {
    var isRefreshing = false
    var onRefresh: () -> Unit = {}
    var thresholdPx = 1f
    private var distancePulled = 0f

    fun reset() {
        distancePulled = 0f
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (source == NestedScrollSource.UserInput && available.y > 0f && !state.isAnimating) {
            pull(available)
        } else {
            Offset.Zero
        }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (source == NestedScrollSource.UserInput && !state.isAnimating) {
            pull(available)
        } else {
            Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (isRefreshing || distancePulled == 0f) return Velocity.Zero
        val triggered = distancePulled * BOTTOM_PULL_DRAG_MULTIPLIER > thresholdPx
        val consumed = if (available.y > 0f) 0f else available.y
        distancePulled = 0f
        if (triggered) {
            onRefresh()
            withFrameNanos {}
        }
        if (!isRefreshing) state.animateToHidden()
        return Velocity(0f, consumed)
    }

    private fun pull(available: Offset): Offset {
        if (isRefreshing) return Offset.Zero
        val pulled = (distancePulled - available.y).coerceAtLeast(0f)
        val consumed = distancePulled - pulled
        if (consumed == 0f) return Offset.Zero
        distancePulled = pulled
        val fraction = indicatorOffset(pulled * BOTTOM_PULL_DRAG_MULTIPLIER) / thresholdPx
        scope.launch {
            if (!state.isAnimating) state.snapTo(fraction)
        }
        return Offset(0f, consumed)
    }

    private fun indicatorOffset(adjusted: Float): Float {
        if (adjusted <= thresholdPx) return adjusted
        val tension = (adjusted / thresholdPx - 1f).coerceIn(0f, 2f)
        return thresholdPx + thresholdPx * (tension - tension * tension / 4f)
    }
}

private const val BOTTOM_PULL_DRAG_MULTIPLIER = 0.5f
