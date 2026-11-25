package dev.dimension.flare.data.datastore.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class TimelineNewestAtBottomTest {
    @Serializable
    private data class PreviousSettings(
        val version: String,
        val aiConfig: AppSettings.AiConfig = AppSettings.AiConfig(),
        val language: String = "",
        val translateConfig: AppSettings.TranslateConfig = AppSettings.TranslateConfig(),
        val linkOpenDefaults: AppSettings.LinkOpenDefaults = AppSettings.LinkOpenDefaults(),
        val mxgaEnabled: Boolean = false,
        val refreshHomeTimelineOnLaunch: Boolean = true,
        val homeTimelineAutoRefreshInterval: TimelineAutoRefreshInterval = TimelineAutoRefreshInterval.DISABLED,
        val rememberHomeTimelinePosition: Boolean = false,
        val imageTranslationConfig: ImageTranslationConfig = ImageTranslationConfig(),
        val videoPlaybackSpeed: Float = 1f,
    )

    @Test
    fun oldSettingsKeepNewestPostsAtTheTop() {
        val old = PreviousSettings("old", rememberHomeTimelinePosition = true, videoPlaybackSpeed = 1.5f)
        val decoded = ProtoBuf.decodeFromByteArray(AppSettings.serializer(), ProtoBuf.encodeToByteArray(PreviousSettings.serializer(), old))
        assertFalse(decoded.timelineNewestAtBottom)
        assertTrue(decoded.rememberHomeTimelinePosition)
        assertEquals(1.5f, decoded.videoPlaybackSpeed)
        assertFalse(Json.decodeFromString(AppSettings.serializer(), """{"version":"old"}""").timelineNewestAtBottom)
    }

    @Test
    fun protobufPreservesNewestPostsAtTheBottom() {
        val settings = AppSettings(version = "test", timelineNewestAtBottom = true)
        val restored = ProtoBuf.decodeFromByteArray(AppSettings.serializer(), ProtoBuf.encodeToByteArray(AppSettings.serializer(), settings))
        assertEquals(settings, restored)
    }
}
