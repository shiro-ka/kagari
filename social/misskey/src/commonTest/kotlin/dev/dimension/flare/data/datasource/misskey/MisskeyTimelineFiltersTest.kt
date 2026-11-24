package dev.dimension.flare.data.datasource.misskey

import dev.dimension.flare.data.network.misskey.MisskeyService
import dev.dimension.flare.model.MicroBlogKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MisskeyTimelineFiltersTest {
    private val accountKey = MicroBlogKey("account", "example.com")
    private val service = MisskeyService(baseUrl = "https://example.com/api/")

    @Test
    fun defaultValueMatchesApiDefaults() {
        listOf(
            MisskeyTimelineSwitches.home,
            MisskeyTimelineSwitches.local,
            MisskeyTimelineSwitches.hybrid,
            MisskeyTimelineSwitches.global,
        ).forEach { switch ->
            assertEquals(MisskeyTimelineFilters(), switch.misskeyTimelineFilters(switch.defaultValue))
        }
    }

    @Test
    fun homeAndGlobalDoNotOfferReplies() {
        val replies = MisskeyTimelineFilter.Replies.value
        listOf(MisskeyTimelineSwitches.home, MisskeyTimelineSwitches.global).forEach { switch ->
            assertFalse(switch.options.any { it.value == replies })
        }
        listOf(MisskeyTimelineSwitches.local, MisskeyTimelineSwitches.hybrid).forEach { switch ->
            assertTrue(switch.options.any { it.value == replies })
        }
    }

    @Test
    fun selectedValuesBecomeRequestFlags() {
        val switch = MisskeyTimelineSwitches.local
        val value =
            switch.select(
                switch.select(switch.defaultValue, MisskeyTimelineFilter.Renotes.value),
                MisskeyTimelineFilter.Files.value,
            )
        assertEquals(
            MisskeyTimelineFilters(withRenotes = false, withReplies = false, withFiles = true),
            switch.misskeyTimelineFilters(value),
        )
    }

    @Test
    fun pagingKeyDependsOnFilters() {
        val all = LocalTimelineRemoteMediator(accountKey, service)
        val filesOnly =
            LocalTimelineRemoteMediator(
                accountKey,
                service,
                MisskeyTimelineFilters(withRenotes = false, withFiles = true),
            )
        assertNotEquals(all.pagingKey, filesOnly.pagingKey)
    }
}
