package dev.dimension.flare.data.datasource.microblog

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public data class KagariPlatformPostActionLayouts(
    val layouts: Map<String, PostActionLayoutConfig> = emptyMap(),
) {
    public fun layoutFor(
        platformId: String?,
        fallback: PostActionLayoutConfig,
    ): PostActionLayoutConfig = platformId?.let { layouts[it] } ?: fallback

    public fun with(
        platformId: String,
        config: PostActionLayoutConfig,
    ): KagariPlatformPostActionLayouts = copy(layouts = layouts + (platformId to config))

    public companion object {
        public val Default: KagariPlatformPostActionLayouts = KagariPlatformPostActionLayouts()
    }
}

public object KagariPostActionPlatforms {
    public const val BLUESKY: String = "Bluesky"
    public const val XQT: String = "xQt"
    public const val MASTODON: String = "Mastodon"
    public const val MISSKEY: String = "Misskey"
    public const val NOSTR: String = "Nostr"
    public const val VVO: String = "VVo"
    public const val PIXIV: String = "Pixiv"
    public const val FANBOX: String = "Fanbox"

    private val familiesByPlatform: Map<String, Set<PostActionFamily>> =
        mapOf(
            BLUESKY to
                setOf(
                    PostActionFamily.Reply,
                    PostActionFamily.Repost,
                    PostActionFamily.Like,
                    PostActionFamily.Translate,
                    PostActionFamily.Bookmark,
                    PostActionFamily.Share,
                    PostActionFamily.Delete,
                    PostActionFamily.Report,
                    PostActionFamily.MuteUser,
                    PostActionFamily.BlockUser,
                ),
            XQT to
                setOf(
                    PostActionFamily.Reply,
                    PostActionFamily.Repost,
                    PostActionFamily.Like,
                    PostActionFamily.Translate,
                    PostActionFamily.Bookmark,
                    PostActionFamily.Share,
                    PostActionFamily.Delete,
                    PostActionFamily.Report,
                    PostActionFamily.MuteUser,
                    PostActionFamily.BlockUser,
                ),
            MASTODON to
                setOf(
                    PostActionFamily.Reply,
                    PostActionFamily.Repost,
                    PostActionFamily.Like,
                    PostActionFamily.React,
                    PostActionFamily.Translate,
                    PostActionFamily.Bookmark,
                    PostActionFamily.Share,
                    PostActionFamily.Delete,
                    PostActionFamily.Report,
                    PostActionFamily.MuteUser,
                    PostActionFamily.BlockUser,
                ),
            MISSKEY to
                setOf(
                    PostActionFamily.Reply,
                    PostActionFamily.Repost,
                    PostActionFamily.React,
                    PostActionFamily.Translate,
                    PostActionFamily.Favorite,
                    PostActionFamily.Share,
                    PostActionFamily.Delete,
                    PostActionFamily.Report,
                    PostActionFamily.MuteUser,
                    PostActionFamily.BlockUser,
                ),
            NOSTR to
                setOf(
                    PostActionFamily.Reply,
                    PostActionFamily.Repost,
                    PostActionFamily.Like,
                    PostActionFamily.Translate,
                    PostActionFamily.Share,
                    PostActionFamily.Delete,
                    PostActionFamily.Report,
                    PostActionFamily.MuteUser,
                    PostActionFamily.BlockUser,
                ),
            VVO to
                setOf(
                    PostActionFamily.Comment,
                    PostActionFamily.Repost,
                    PostActionFamily.Like,
                    PostActionFamily.Translate,
                    PostActionFamily.Favorite,
                    PostActionFamily.Share,
                    PostActionFamily.Delete,
                    PostActionFamily.Report,
                ),
            PIXIV to
                setOf(
                    PostActionFamily.Favorite,
                    PostActionFamily.Share,
                ),
            FANBOX to
                setOf(
                    PostActionFamily.Like,
                ),
        )

    public fun familiesFor(platformId: String?): List<PostActionFamily> {
        val families = platformId?.let { familiesByPlatform[it] } ?: return PostActionLayoutHelpers.allEditableFamilies
        return PostActionLayoutHelpers.allEditableFamilies.filter { it in families }
    }
}
