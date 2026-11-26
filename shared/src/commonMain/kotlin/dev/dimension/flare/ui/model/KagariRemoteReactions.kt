package dev.dimension.flare.ui.model

import dev.dimension.flare.data.datasource.microblog.PostEvent
import dev.dimension.flare.data.model.KagariRemoteReactionMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val KAGARI_HEART_REACTION = "❤"

private val kagariHeartReactions = setOf("❤", "❤️", "♥", "♥️")

public val UiTimelineV2.Post.EmojiReaction.isKagariRemoteCustomEmoji: Boolean
    get() {
        if (isUnicode || name.length < 3 || !name.startsWith(":") || !name.endsWith(":")) return false
        val host = name.substring(1, name.length - 1).substringAfterLast('@', "")
        return host.isNotEmpty() && host != "."
    }

public val UiTimelineV2.Post.EmojiReaction.isKagariReactable: Boolean
    get() = !isKagariRemoteCustomEmoji

public fun List<UiTimelineV2.Post.EmojiReaction>.kagariAvailableFirst(): ImmutableList<UiTimelineV2.Post.EmojiReaction> {
    val (available, unavailable) = partition { it.isKagariReactable }
    return (available + unavailable).toImmutableList()
}

public fun List<UiTimelineV2.Post.EmojiReaction>.kagariWithRemoteReactionMode(
    mode: KagariRemoteReactionMode,
): ImmutableList<UiTimelineV2.Post.EmojiReaction> {
    if (mode == KagariRemoteReactionMode.AsIs || none { it.isKagariRemoteCustomEmoji }) {
        return toImmutableList()
    }
    val (remote, rest) = partition { it.isKagariRemoteCustomEmoji }
    if (mode == KagariRemoteReactionMode.Hidden) {
        return rest.toImmutableList()
    }
    val heart = rest.firstOrNull { it.isUnicode && it.name in kagariHeartReactions }
    val members = listOfNotNull(heart) + remote
    val merged =
        UiTimelineV2.Post.EmojiReaction(
            name = heart?.name ?: KAGARI_HEART_REACTION,
            url = heart?.url.orEmpty(),
            count = UiNumber(members.sumOf { it.count.value }),
            clickEvent =
                members.firstOrNull { it.me }?.clickEvent
                    ?: heart?.clickEvent
                    ?: remote.first().clickEvent.kagariAsHeartReaction(),
            isUnicode = true,
            me = members.any { it.me },
            proxy = heart?.proxy ?: remote.first().proxy,
        )
    return (rest.filter { it !== heart } + merged)
        .sortedByDescending { it.count.value }
        .toImmutableList()
}

private fun ClickEvent.kagariAsHeartReaction(): ClickEvent {
    val target = postEventOrNull() ?: return this
    val event = target.postEvent as? PostEvent.Misskey.React ?: return this
    return ClickEvent.event(
        target.accountKey,
        event.copy(hasReacted = false, reaction = KAGARI_HEART_REACTION),
    )
}
