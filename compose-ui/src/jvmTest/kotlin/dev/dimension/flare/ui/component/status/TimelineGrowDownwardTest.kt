package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class TimelineGrowDownwardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class Placed(
        val top: Float,
        val bottom: Float,
    )

    private fun run(
        reverseLayout: Boolean,
        initialOffset: Int,
        grow: Int,
        check: (before: Placed, after: Placed) -> Unit,
    ) {
        val heights = mutableStateMapOf<Int, Int>()
        val placed = mutableMapOf<Int, Placed>()
        val scroll = LazyStaggeredGridState()
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                CompositionLocalProvider(LocalReversedTimelineState provides if (reverseLayout) scroll else null) {
                    LazyVerticalStaggeredGrid(
                        StaggeredGridCells.Fixed(1),
                        Modifier.size(300.dp, 400.dp),
                        state = scroll,
                        reverseLayout = reverseLayout,
                    ) {
                        items(20, key = { it }) { index ->
                            Box(
                                Modifier
                                    .growDownwardInReversedTimeline()
                                    .onGloballyPositioned {
                                        val top = it.positionInParent().y
                                        placed[index] = Placed(top, top + it.size.height)
                                    }.fillMaxWidth()
                                    .height((heights[index] ?: 100).dp),
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        runBlocking { scroll.scrollToItem(0, initialOffset) }
        composeRule.waitForIdle()
        val before = placed.getValue(grow)
        composeRule.runOnIdle { heights[grow] = 200 }
        composeRule.waitForIdle()
        check(before, placed.getValue(grow))
    }

    @Test
    fun reversedPostWithVisibleTopGrowsDownward() =
        run(reverseLayout = true, initialOffset = 0, grow = 2) { before, after ->
            assertEquals(100f, before.top)
            assertEquals(before.top, after.top)
            assertEquals(before.bottom + 100f, after.bottom)
        }

    @Test
    fun reversedNewestPostGrowsDownward() =
        run(reverseLayout = true, initialOffset = 0, grow = 1) { before, after ->
            assertEquals(before.top, after.top)
            assertEquals(before.bottom + 100f, after.bottom)
        }

    @Test
    fun reversedPostCutOffAtTheTopKeepsItsBottom() =
        run(reverseLayout = true, initialOffset = 50, grow = 4) { before, after ->
            assertEquals(-50f, before.top)
            assertEquals(before.bottom, after.bottom)
        }

    @Test
    fun normalTimelineIsUnchanged() =
        run(reverseLayout = false, initialOffset = 0, grow = 1) { before, after ->
            assertEquals(before.top, after.top)
            assertEquals(before.bottom + 100f, after.bottom)
        }
}
