package dev.dimension.flare.data.datasource.xqt

import dev.dimension.flare.data.network.xqt.model.Description
import dev.dimension.flare.data.network.xqt.model.Entities
import dev.dimension.flare.data.network.xqt.model.User
import dev.dimension.flare.data.network.xqt.model.UserLegacy
import dev.dimension.flare.data.network.xqt.model.UserLocation
import dev.dimension.flare.data.network.xqt.model.XqtUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KagariTwitterProfileDraftTest {
    @Test
    fun shortLinksAreExpandedAndEntitiesDecoded() {
        val draft =
            User(
                restId = "1",
                legacy =
                    UserLegacy(
                        name = "しろか &amp; kagari",
                        description = "作ってます https://t.co/abc と https://t.co/def",
                        location = "東京",
                        url = "https://t.co/xyz",
                        profileImageUrlHttps = "https://pbs.twimg.com/profile_images/1/a_normal.jpg",
                        profileBannerUrl = "https://pbs.twimg.com/profile_banners/1/2",
                        entities =
                            Entities(
                                description =
                                    Description(
                                        urls =
                                            listOf(
                                                XqtUrl(url = "https://t.co/abc", expandedUrl = "https://example.com/a"),
                                                XqtUrl(url = "https://t.co/def", expandedUrl = null),
                                            ),
                                    ),
                                url = Description(urls = listOf(XqtUrl(url = "https://t.co/xyz", expandedUrl = "https://shiro-ka.github.io"))),
                            ),
                    ),
            ).kagariProfileDraft()
        assertEquals("しろか & kagari", draft.name)
        assertEquals("作ってます https://example.com/a と https://t.co/def", draft.description)
        assertEquals("東京", draft.location)
        assertEquals("https://shiro-ka.github.io", draft.url)
        assertEquals("https://pbs.twimg.com/profile_images/1/a.jpg", draft.avatarUrl)
        assertEquals("https://pbs.twimg.com/profile_banners/1/2", draft.bannerUrl)
    }

    @Test
    fun missingFieldsBecomeEmpty() {
        val draft = User(restId = "1", legacy = UserLegacy(name = "a"), location = UserLocation(location = "大阪")).kagariProfileDraft()
        assertEquals("", draft.description)
        assertEquals("大阪", draft.location)
        assertEquals("", draft.url)
        assertNull(draft.bannerUrl)
    }
}
