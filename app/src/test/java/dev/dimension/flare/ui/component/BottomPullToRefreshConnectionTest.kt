package dev.dimension.flare.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
class BottomPullToRefreshConnectionTest {
    private class FakeState : PullToRefreshState {
        override var distanceFraction = 0f
        override val isAnimating = false
        var hidden = 0

        override suspend fun animateToThreshold() {
            distanceFraction = 1f
        }

        override suspend fun animateToHidden() {
            hidden++
            distanceFraction = 0f
        }

        override suspend fun snapTo(targetValue: Float) {
            distanceFraction = targetValue
        }
    }

    private val frameClock =
        object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
        }

    @Test
    fun pullingUpAtTheBottomEdgeRefreshesOnRelease() =
        runTest(UnconfinedTestDispatcher()) {
            val state = FakeState()
            var refreshed = 0
            val connection = BottomPullToRefreshConnection(state, this)
            connection.thresholdPx = 100f
            connection.onRefresh = { refreshed++ }
            val consumed = connection.onPostScroll(Offset.Zero, Offset(0f, -250f), NestedScrollSource.UserInput)
            assertEquals(-250f, consumed.y)
            assertTrue(state.distanceFraction > 1f)
            withContext(frameClock) { connection.onPreFling(Velocity(0f, -10f)) }
            assertEquals(1, refreshed)
        }

    @Test
    fun pullingDownAtRestIsLeftToTheList() =
        runTest(UnconfinedTestDispatcher()) {
            val state = FakeState()
            val connection = BottomPullToRefreshConnection(state, this)
            assertEquals(Offset.Zero, connection.onPreScroll(Offset(0f, 30f), NestedScrollSource.UserInput))
            assertEquals(0f, connection.onPostScroll(Offset.Zero, Offset(0f, 30f), NestedScrollSource.UserInput).y)
            assertEquals(Velocity.Zero, connection.onPreFling(Velocity(0f, 500f)))
            assertEquals(0f, state.distanceFraction)
        }

    @Test
    fun pushingBackDownBeforeTheThresholdCancelsTheRefresh() =
        runTest(UnconfinedTestDispatcher()) {
            val state = FakeState()
            var refreshed = 0
            val connection = BottomPullToRefreshConnection(state, this)
            connection.thresholdPx = 100f
            connection.onRefresh = { refreshed++ }
            connection.onPostScroll(Offset.Zero, Offset(0f, -120f), NestedScrollSource.UserInput)
            assertEquals(80f, connection.onPreScroll(Offset(0f, 80f), NestedScrollSource.UserInput).y)
            withContext(frameClock) { connection.onPreFling(Velocity.Zero) }
            assertEquals(0, refreshed)
            assertEquals(1, state.hidden)
        }

    @Test
    fun pullingWhileRefreshingIsIgnored() =
        runTest(UnconfinedTestDispatcher()) {
            val state = FakeState()
            val connection = BottomPullToRefreshConnection(state, this)
            connection.isRefreshing = true
            assertEquals(0f, connection.onPostScroll(Offset.Zero, Offset(0f, -100f), NestedScrollSource.UserInput).y)
            assertFalse(state.distanceFraction > 0f)
        }
}
