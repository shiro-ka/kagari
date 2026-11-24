package dev.dimension.flare.ui.model.mapper

import dev.dimension.flare.data.datasource.microblog.ActionMenu
import dev.dimension.flare.data.datasource.microblog.PostActionFamily
import dev.dimension.flare.data.datasource.microblog.PostEvent
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.ClickEvent
import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.model.UiNumber
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.KagariPixivBookmarkRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

@Serializable
public enum class PixivBookmarkState {
    None,
    Bookmarked,
    Public,
    Private,
}

@Serializable
public enum class PixivBookmarkKind {
    MatchFollow,
    Public,
    Private,
}

public val PixivBookmarkState.isBookmarked: Boolean
    get() = this != PixivBookmarkState.None

public fun ActionMenu.Companion.pixivFavourite(
    statusKey: MicroBlogKey,
    favourited: Boolean,
    count: Long,
    accountKey: MicroBlogKey,
): ActionMenu.Item =
    pixivBookmark(
        kind = PixivBookmarkKind.MatchFollow,
        statusKey = statusKey,
        state = if (favourited) PixivBookmarkState.Bookmarked else PixivBookmarkState.None,
        count = count,
        accountKey = accountKey,
        authorKey = null,
    )

public fun ActionMenu.Companion.pixivBookmark(
    kind: PixivBookmarkKind,
    statusKey: MicroBlogKey,
    state: PixivBookmarkState,
    count: Long,
    accountKey: MicroBlogKey,
    authorKey: MicroBlogKey?,
): ActionMenu.Item {
    val active =
        when (kind) {
            PixivBookmarkKind.MatchFollow -> state.isBookmarked
            PixivBookmarkKind.Public -> state == PixivBookmarkState.Public
            PixivBookmarkKind.Private -> state == PixivBookmarkState.Private
        }
    return ActionMenu.Item(
        updateKey = pixivBookmarkUpdateKey(kind, statusKey),
        icon =
            when (kind) {
                PixivBookmarkKind.MatchFollow,
                PixivBookmarkKind.Public,
                -> if (active) UiIcon.Unlike else UiIcon.Like

                PixivBookmarkKind.Private -> if (active) UiIcon.KagariPrivateBookmarked else UiIcon.KagariPrivateBookmark
            },
        text =
            ActionMenu.Item.Text.Localized(
                when (kind) {
                    PixivBookmarkKind.MatchFollow -> {
                        if (active) ActionMenu.Item.Text.Localized.Type.UnFavorite else ActionMenu.Item.Text.Localized.Type.Favorite
                    }

                    PixivBookmarkKind.Public -> {
                        if (active) {
                            ActionMenu.Item.Text.Localized.Type.KagariPixivRemoveBookmark
                        } else {
                            ActionMenu.Item.Text.Localized.Type.KagariPixivBookmarkPublic
                        }
                    }

                    PixivBookmarkKind.Private -> {
                        if (active) {
                            ActionMenu.Item.Text.Localized.Type.KagariPixivRemoveBookmark
                        } else {
                            ActionMenu.Item.Text.Localized.Type.KagariPixivBookmarkPrivate
                        }
                    }
                },
            ),
        count = if (kind == PixivBookmarkKind.MatchFollow) UiNumber(count) else null,
        color = if (active) ActionMenu.Item.Color.Red else null,
        clickEvent =
            ClickEvent.event(
                accountKey,
                PostEvent.Pixiv.Bookmark(
                    postKey = statusKey,
                    bookmarked = state.isBookmarked,
                    count = count,
                    accountKey = accountKey,
                    kind = kind,
                    state = state,
                    authorKey = authorKey,
                ),
            ),
        actionFamily =
            when (kind) {
                PixivBookmarkKind.MatchFollow -> PostActionFamily.Favorite
                PixivBookmarkKind.Public -> PostActionFamily.BookmarkPublic
                PixivBookmarkKind.Private -> PostActionFamily.BookmarkPrivate
            },
    )
}

public fun ActionMenu.Companion.pixivFollow(
    isPrivate: Boolean,
    statusKey: MicroBlogKey,
    userKey: MicroBlogKey,
    followed: Boolean,
    accountKey: MicroBlogKey,
): ActionMenu.Item =
    ActionMenu.Item(
        updateKey = pixivFollowUpdateKey(isPrivate, statusKey),
        icon =
            when {
                followed -> UiIcon.KagariFollowing
                isPrivate -> UiIcon.KagariFollowPrivate
                else -> UiIcon.Follow
            },
        text =
            ActionMenu.Item.Text.Localized(
                when {
                    followed -> ActionMenu.Item.Text.Localized.Type.KagariPixivUnfollow
                    isPrivate -> ActionMenu.Item.Text.Localized.Type.KagariPixivFollowPrivate
                    else -> ActionMenu.Item.Text.Localized.Type.KagariPixivFollow
                },
            ),
        color = if (followed) ActionMenu.Item.Color.PrimaryColor else null,
        clickEvent =
            ClickEvent.event(
                accountKey,
                PostEvent.Pixiv.Follow(
                    postKey = statusKey,
                    accountKey = accountKey,
                    userKey = userKey,
                    followed = followed,
                    isPrivate = isPrivate,
                ),
            ),
        actionFamily = if (isPrivate) PostActionFamily.FollowPrivate else PostActionFamily.Follow,
    )

public fun ActionMenu.Companion.pixivSave(statusKey: MicroBlogKey): ActionMenu.Item =
    ActionMenu.Item(
        updateKey = "pixiv_save_$statusKey",
        icon = UiIcon.KagariSave,
        text = ActionMenu.Item.Text.Localized(ActionMenu.Item.Text.Localized.Type.KagariSave),
        clickEvent = ClickEvent.Noop,
        actionFamily = PostActionFamily.Save,
    )

public fun ActionMenu.Companion.pixivOpenInBrowser(
    statusKey: MicroBlogKey,
    url: String,
): ActionMenu.Item =
    ActionMenu.Item(
        updateKey = "pixiv_open_in_browser_$statusKey",
        icon = UiIcon.World,
        text = ActionMenu.Item.Text.Localized(ActionMenu.Item.Text.Localized.Type.KagariOpenInBrowser),
        clickEvent = ClickEvent.Deeplink(DeeplinkRoute.OpenLinkDirectly(url)),
        actionFamily = PostActionFamily.OpenInBrowser,
    )

public fun ImmutableList<ActionMenu>.withPixivBookmarkState(
    statusKey: MicroBlogKey,
    accountKey: MicroBlogKey,
    authorKey: MicroBlogKey?,
    state: PixivBookmarkState,
    count: Long,
): ImmutableList<ActionMenu> =
    replacePixivItems { item ->
        PixivBookmarkKind.entries
            .firstOrNull { item.updateKey == pixivBookmarkUpdateKey(it, statusKey) }
            ?.let { kind ->
                ActionMenu.pixivBookmark(
                    kind = kind,
                    statusKey = statusKey,
                    state = state,
                    count = count,
                    accountKey = accountKey,
                    authorKey = authorKey,
                )
            }
    }

public fun ImmutableList<ActionMenu>.withPixivFollowState(
    statusKey: MicroBlogKey,
    accountKey: MicroBlogKey,
    userKey: MicroBlogKey,
    followed: Boolean,
): ImmutableList<ActionMenu> =
    replacePixivItems { item ->
        listOf(false, true)
            .firstOrNull { item.updateKey == pixivFollowUpdateKey(it, statusKey) }
            ?.let { isPrivate ->
                ActionMenu.pixivFollow(
                    isPrivate = isPrivate,
                    statusKey = statusKey,
                    userKey = userKey,
                    followed = followed,
                    accountKey = accountKey,
                )
            }
    }

public fun ImmutableList<ActionMenu>.pixivBookmarkCount(statusKey: MicroBlogKey): Long? =
    firstNotNullOfOrNull { action ->
        when (action) {
            is ActionMenu.Item -> action.takeIf { it.updateKey == pixivBookmarkUpdateKey(PixivBookmarkKind.MatchFollow, statusKey) }?.count?.value
            is ActionMenu.Group -> action.actions.pixivBookmarkCount(statusKey)
            ActionMenu.Divider -> null
        }
    }

public fun UiTimelineV2.Post.kagariPixivLongClickEvent(item: ActionMenu.Item): ClickEvent? =
    PixivBookmarkKind.entries
        .firstOrNull { item.updateKey == pixivBookmarkUpdateKey(it, statusKey) }
        ?.let {
            ClickEvent.Deeplink(
                KagariPixivBookmarkRoute(
                    statusKey = statusKey,
                    accountType = accountType,
                ),
            )
        }

private fun ImmutableList<ActionMenu>.replacePixivItems(replace: (ActionMenu.Item) -> ActionMenu.Item?): ImmutableList<ActionMenu> =
    map { action ->
        when (action) {
            is ActionMenu.Item -> replace(action) ?: action
            is ActionMenu.Group -> action.copy(actions = action.actions.replacePixivItems(replace))
            ActionMenu.Divider -> action
        }
    }.toImmutableList()

private fun pixivBookmarkUpdateKey(
    kind: PixivBookmarkKind,
    statusKey: MicroBlogKey,
): String =
    when (kind) {
        PixivBookmarkKind.MatchFollow -> "pixiv_bookmark_$statusKey"
        PixivBookmarkKind.Public -> "pixiv_bookmark_public_$statusKey"
        PixivBookmarkKind.Private -> "pixiv_bookmark_private_$statusKey"
    }

private fun pixivFollowUpdateKey(
    isPrivate: Boolean,
    statusKey: MicroBlogKey,
): String = if (isPrivate) "pixiv_follow_private_$statusKey" else "pixiv_follow_$statusKey"
