package dev.dimension.flare.ui.model.mapper

import dev.dimension.flare.data.network.xqt.XQT_JSON
import dev.dimension.flare.data.network.xqt.model.KagariSearchTypeahead
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiProfile
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.modules
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XQTTypeaheadMapperTest {
    private val accountKey = MicroBlogKey("1", "x.com")

    @BeforeTest
    fun setup() {
        startKoin {
            modules(XqtRenderTestModule::class)
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun typeaheadUsersRenderAsMentionCandidates() {
        val response =
            XQT_JSON.decodeFromString<KagariSearchTypeahead>(
                """
                {"num_results":3,"query":"kag","topics":[],"events":[],"lists":[],"users":[
                  {"id":44,"id_str":"44","name":"Kagari","screen_name":"kagari_app",
                   "profile_image_url_https":"https://pbs.twimg.com/profile_images/1/a_normal.jpg",
                   "verified":false,"ext_is_blue_verified":true,"is_protected":true,"location":""},
                  {"id_str":"45","screen_name":"plain"},
                  {"id_str":"","screen_name":"broken"}
                ]}
                """.trimIndent(),
            )
        val profiles = response.users.mapNotNull { it.render(accountKey) }
        assertEquals(listOf("44", "45"), profiles.map { it.key.id })
        val first = profiles.first()
        assertEquals("kagari_app", first.handle.raw)
        assertEquals("https://pbs.twimg.com/profile_images/1/a.jpg", first.avatar?.url)
        assertEquals(listOf(UiProfile.Mark.Verified, UiProfile.Mark.Locked), first.mark)
        assertEquals(emptyList(), profiles.last().mark)
        assertEquals("plain", profiles.last().handle.raw)
        assertNull(response.users.last().render(accountKey))
    }
}
