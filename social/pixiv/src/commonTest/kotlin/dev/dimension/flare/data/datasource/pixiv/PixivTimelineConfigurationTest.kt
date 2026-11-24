package dev.dimension.flare.data.datasource.pixiv

import dev.dimension.flare.data.network.pixiv.PixivRankingMode
import dev.dimension.flare.data.network.pixiv.PixivRestrict
import dev.dimension.flare.data.network.pixiv.PixivService
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiStrings
import dev.dimension.flare.ui.model.asText
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PixivTimelineConfigurationTest {
    @Test
    fun additionalRankingsAreOnlyInTheTabPicker() {
        val account = MicroBlogKey("test", "pixiv.net")
        val source = PixivDataSource(account, emptyFlow()) {}
        val builtInIds = source.builtInTimelineTabs.map { it.target.spec.id }
        val newIds =
            listOf(
                "pixiv.ranking.day_ai",
                "pixiv.ranking.day_r18",
                "pixiv.ranking.week_r18",
                "pixiv.ranking.day_r18_ai",
                "pixiv.ranking.week_r18g",
            )

        assertTrue(builtInIds.none { it in newIds })
        assertEquals(builtInIds + newIds, source.addableTimelineTabs.map { it.target.spec.id })
    }

    @Test
    fun tabPickerSectionsCoverEveryAddableTab() {
        val account = MicroBlogKey("test", "pixiv.net")
        val source = PixivDataSource(account, emptyFlow()) {}
        val sections = source.kagariAddableTimelineTabSections
        assertEquals(
            listOf(
                UiStrings.KagariPixivTabSectionUser.asText(),
                UiStrings.KagariPixivTabSectionRanking.asText(),
                UiStrings.KagariPixivTabSectionRankingR18.asText(),
            ),
            sections.map { it.title },
        )
        assertEquals(
            source.addableTimelineTabs.map { it.target.spec.id },
            sections.flatMap { section -> section.tabs.map { it.target.spec.id } },
        )
        assertEquals(
            listOf(
                "pixiv.ranking.day_r18",
                "pixiv.ranking.week_r18",
                "pixiv.ranking.day_r18_ai",
                "pixiv.ranking.week_r18g",
            ),
            sections.last().tabs.map { it.target.spec.id },
        )
    }

    @Test
    fun followingFeedsCanJoinButRecommendationsCannot() {
        val service = PixivService()
        val account = MicroBlogKey("test", "pixiv.net")
        assertTrue(PixivHomeTimelineLoader(service, account).canJoinTimeline)
        for (restrict in PixivRestrict.entries) {
            assertTrue(PixivFollowingTimelineLoader(service, account, restrict).canJoinTimeline)
        }
        assertFalse(PixivDiscoverTimelineLoader(service, account).canJoinTimeline)
        for (mode in PixivRankingMode.entries) {
            assertFalse(PixivRankingTimelineLoader(service, account, mode).canJoinTimeline)
        }
    }
}
