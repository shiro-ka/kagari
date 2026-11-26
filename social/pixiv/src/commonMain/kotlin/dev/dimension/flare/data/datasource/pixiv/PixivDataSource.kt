package dev.dimension.flare.data.datasource.pixiv

import dev.dimension.flare.common.Cacheable
import dev.dimension.flare.data.datasource.microblog.AuthenticatedMicroblogDataSource
import dev.dimension.flare.data.datasource.microblog.DatabaseUpdater
import dev.dimension.flare.data.datasource.microblog.KagariSearchKind
import dev.dimension.flare.data.datasource.microblog.KagariSearchKinds
import dev.dimension.flare.data.datasource.microblog.PostEvent
import dev.dimension.flare.data.datasource.microblog.ProfileTab
import dev.dimension.flare.data.datasource.microblog.datasource.GalleryDataSource
import dev.dimension.flare.data.datasource.microblog.datasource.GalleryDetail
import dev.dimension.flare.data.datasource.microblog.datasource.KagariAddableTimelineTabSection
import dev.dimension.flare.data.datasource.microblog.datasource.KagariPixivBookmarkDataSource
import dev.dimension.flare.data.datasource.microblog.datasource.KagariPixivBookmarkDetail
import dev.dimension.flare.data.datasource.microblog.datasource.KagariPixivBookmarkTag
import dev.dimension.flare.data.datasource.microblog.datasource.PinnableTimelineTabDataSource
import dev.dimension.flare.data.datasource.microblog.datasource.PinnableTimelineTabSection
import dev.dimension.flare.data.datasource.microblog.datasource.PostDataSource
import dev.dimension.flare.data.datasource.microblog.datasource.RelationDataSource
import dev.dimension.flare.data.datasource.microblog.datasource.TimelineTabConfigurationDataSource
import dev.dimension.flare.data.datasource.microblog.datasource.UserDataSource
import dev.dimension.flare.data.datasource.microblog.handler.PostEventHandler
import dev.dimension.flare.data.datasource.microblog.handler.PostHandler
import dev.dimension.flare.data.datasource.microblog.handler.RelationHandler
import dev.dimension.flare.data.datasource.microblog.handler.UserHandler
import dev.dimension.flare.data.datasource.microblog.loader.RelationActionType
import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
import dev.dimension.flare.data.datasource.microblog.paging.ShareRepliesLoader
import dev.dimension.flare.data.datasource.microblog.paging.notSupported
import dev.dimension.flare.data.model.IconType
import dev.dimension.flare.data.model.ProxyRef
import dev.dimension.flare.data.model.tab.ShortcutSpec
import dev.dimension.flare.data.model.tab.TimelineCandidate
import dev.dimension.flare.data.model.tab.TimelineSpec
import dev.dimension.flare.data.network.pixiv.PixivFollowRestrict
import dev.dimension.flare.data.network.pixiv.PixivRankingMode
import dev.dimension.flare.data.network.pixiv.PixivRestrict
import dev.dimension.flare.data.network.pixiv.PixivSearchSort
import dev.dimension.flare.data.network.pixiv.PixivService
import dev.dimension.flare.data.network.pixiv.PixivWorkType
import dev.dimension.flare.data.platform.CommonTimelineSpecs
import dev.dimension.flare.data.platform.PixivCredential
import dev.dimension.flare.data.platform.PixivPlatformSpec
import dev.dimension.flare.media.UgoiraFrame
import dev.dimension.flare.media.UgoiraMetadata
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiHashtag
import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.model.UiStrings
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.model.asText
import dev.dimension.flare.ui.model.contentPostOrNull
import dev.dimension.flare.ui.model.mapper.PixivBookmarkKind
import dev.dimension.flare.ui.model.mapper.PixivBookmarkState
import dev.dimension.flare.ui.model.mapper.PixivPrivateFollows
import dev.dimension.flare.ui.model.mapper.isBookmarked
import dev.dimension.flare.ui.model.mapper.pixivBookmarkCount
import dev.dimension.flare.ui.model.mapper.withPixivBookmarkState
import dev.dimension.flare.ui.model.mapper.withPixivFollowState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class PixivDataSource(
    override val accountKey: MicroBlogKey,
    private val credentialFlow: Flow<PixivCredential>,
    private val updateCredential: suspend (PixivCredential) -> Unit,
) : AuthenticatedMicroblogDataSource,
    PinnableTimelineTabDataSource,
    TimelineTabConfigurationDataSource,
    UserDataSource,
    GalleryDataSource,
    PostDataSource,
    RelationDataSource,
    KagariPixivBookmarkDataSource,
    KagariSearchKinds,
    PostEventHandler.Handler {
    private val service =
        PixivService(
            accountKey = accountKey,
            credentialFlow = credentialFlow,
            onCredentialRefreshed = updateCredential,
        )
    init {
        PixivPrivateFollows.register(accountKey) { fetchPrivateFollows() }
    }

    private val loader by lazy {
        PixivLoader(
            accountKey = accountKey,
            service = service,
            credentialFlow = credentialFlow,
        )
    }

    override val userHandler by lazy {
        UserHandler(
            proxy = ProxyRef.forAccount(accountKey),
            host = accountKey.host,
            loader = loader,
        )
    }

    override val postHandler by lazy {
        PostHandler(
            accountType = AccountType.Specific(accountKey),
            loader = loader,
        )
    }

    override val postEventHandler by lazy {
        PostEventHandler(
            accountType = AccountType.Specific(accountKey),
            handler = this,
        )
    }

    override val relationHandler by lazy {
        RelationHandler(
            accountType = AccountType.Specific(accountKey),
            dataSource = loader,
        )
    }

    override val supportedRelationTypes: Set<RelationActionType> = loader.supportedTypes

    override val pinnableTimelineTabs: List<PinnableTimelineTabSection> = emptyList()

    private val recommendedTab: TimelineCandidate<*> by lazy {
        CommonTimelineSpecs.discover.galleryCandidate(
            data = TimelineSpec.AccountBasedData(accountKey),
            title = UiStrings.KagariPixivRecommended.asText(),
            icon = UiIcon.KagariWandMagicSparkles.withPixivLogo(),
        )
    }

    private val switchableFollowingTab: TimelineCandidate<*> by lazy {
        PixivPlatformSpec.switchableFollowingTimelineSpec.galleryCandidate(
            TimelineSpec.AccountBasedData(accountKey),
            icon = UiIcon.Home.withPixivLogo(),
        )
    }

    private val switchableBookmarkTab: TimelineCandidate<*> by lazy {
        PixivPlatformSpec.switchableBookmarkTimelineSpec.galleryCandidate(
            TimelineSpec.AccountBasedData(accountKey),
            icon = UiIcon.Heart.withPixivLogo(),
        )
    }

    override val defaultTabs: ImmutableList<TimelineCandidate<*>> by lazy {
        persistentListOf(
            recommendedTab,
            switchableFollowingTab,
            switchableBookmarkTab,
        )
    }

    private val userTimelineTabs: ImmutableList<TimelineCandidate<*>> by lazy {
        val data = TimelineSpec.AccountBasedData(accountKey)
        persistentListOf(
            switchableFollowingTab,
            PixivPlatformSpec.followingTimelineSpec.galleryCandidate(data, icon = UiIcon.Home.withPixivLogo()),
            PixivPlatformSpec.privateFollowingTimelineSpec.galleryCandidate(data, icon = UiIcon.Home.withPixivLogo()),
            recommendedTab,
            switchableBookmarkTab,
            PixivPlatformSpec.bookmarkTimelineSpec.galleryCandidate(data, icon = UiIcon.Heart.withPixivLogo()),
            PixivPlatformSpec.privateBookmarkTimelineSpec.galleryCandidate(data, icon = UiIcon.Heart.withPixivLogo()),
        )
    }

    private val rankingTimelineTabs: ImmutableList<TimelineCandidate<*>> by lazy {
        val data = TimelineSpec.AccountBasedData(accountKey)
        persistentListOf(
            PixivPlatformSpec.rankingDayTimelineSpec,
            PixivPlatformSpec.rankingWeekTimelineSpec,
            PixivPlatformSpec.rankingMonthTimelineSpec,
            PixivPlatformSpec.rankingDayMaleTimelineSpec,
            PixivPlatformSpec.rankingDayFemaleTimelineSpec,
            PixivPlatformSpec.rankingWeekOriginalTimelineSpec,
            PixivPlatformSpec.rankingWeekRookieTimelineSpec,
            PixivPlatformSpec.rankingDayMangaTimelineSpec,
        ).map { it.galleryCandidate(data, icon = rankingIcon) }.toImmutableList()
    }

    private val r18RankingTimelineTabs: ImmutableList<TimelineCandidate<*>> by lazy {
        val data = TimelineSpec.AccountBasedData(accountKey)
        persistentListOf(
            PixivPlatformSpec.rankingDayR18TimelineSpec,
            PixivPlatformSpec.rankingWeekR18TimelineSpec,
            PixivPlatformSpec.rankingWeekR18GTimelineSpec,
        ).map { it.galleryCandidate(data, icon = rankingIcon) }.toImmutableList()
    }

    override val builtInTimelineTabs: ImmutableList<TimelineCandidate<*>> by lazy {
        (userTimelineTabs + rankingTimelineTabs + r18RankingTimelineTabs).toImmutableList()
    }

    override val kagariAddableTimelineTabSections: ImmutableList<KagariAddableTimelineTabSection> by lazy {
        persistentListOf(
            KagariAddableTimelineTabSection(
                title = UiStrings.KagariPixivTabSectionUser,
                tabs = userTimelineTabs,
            ),
            KagariAddableTimelineTabSection(
                title = UiStrings.KagariPixivTabSectionRanking,
                tabs = rankingTimelineTabs,
            ),
            KagariAddableTimelineTabSection(
                title = UiStrings.KagariPixivTabSectionRankingR18,
                tabs = r18RankingTimelineTabs,
            ),
        )
    }

    override val shortcuts: ImmutableList<ShortcutSpec> by lazy {
        persistentListOf(
            ShortcutSpec(
                title = UiStrings.Home,
                icon = UiIcon.Home,
                target =
                    ShortcutSpec.Target.Timeline(
                        CommonTimelineSpecs.home.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                            icon = IconType.Material(UiIcon.Pixiv),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.Discover,
                icon = UiIcon.Search,
                target =
                    ShortcutSpec.Target.Timeline(
                        CommonTimelineSpecs.discover.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                            icon = IconType.Material(UiIcon.Pixiv),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.Following,
                icon = UiIcon.Follow,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.followingTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivPrivateFollowing,
                icon = UiIcon.Follow,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.privateFollowingTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.Favourite,
                icon = UiIcon.Heart,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.bookmarkTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivPrivateFavourites,
                icon = UiIcon.Heart,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.privateBookmarkTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivRankingWeek,
                icon = UiIcon.Featured,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.rankingWeekTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivRankingMonth,
                icon = UiIcon.Featured,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.rankingMonthTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivRankingDayMale,
                icon = UiIcon.Featured,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.rankingDayMaleTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivRankingDayFemale,
                icon = UiIcon.Featured,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.rankingDayFemaleTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivRankingWeekOriginal,
                icon = UiIcon.Featured,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.rankingWeekOriginalTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivRankingWeekRookie,
                icon = UiIcon.Featured,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.rankingWeekRookieTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
            ShortcutSpec(
                title = UiStrings.PixivRankingDayManga,
                icon = UiIcon.Featured,
                target =
                    ShortcutSpec.Target.Timeline(
                        PixivPlatformSpec.rankingDayMangaTimelineSpec.galleryCandidate(
                            data = TimelineSpec.AccountBasedData(accountKey),
                        ),
                    ),
            ),
        )
    }

    override fun homeTimeline(): RemoteLoader<UiTimelineV2> =
        PixivHomeTimelineLoader(
            service = service,
            accountKey = accountKey,
        )

    override fun userTimeline(
        userKey: MicroBlogKey,
        mediaOnly: Boolean,
    ): RemoteLoader<UiTimelineV2> =
        PixivUserTimelineLoader(
            service = service,
            accountKey = accountKey,
            userKey = userKey,
            type = PixivWorkType.Illust,
        )

    override fun shareReplies(statusKey: MicroBlogKey): RemoteLoader<UiTimelineV2> =
        ShareRepliesLoader(galleryComments(statusKey), statusKey, comments = true)

    override fun context(statusKey: MicroBlogKey): RemoteLoader<UiTimelineV2> =
        PixivStatusDetailLoader(
            service = service,
            accountKey = accountKey,
            statusKey = statusKey,
        )

    override suspend fun ugoiraMetadata(statusKey: MicroBlogKey): UgoiraMetadata {
        val metadata = service.ugoiraMetadata(statusKey.id.toLong()).ugoiraMetadata
        return UgoiraMetadata(
            zipUrl = requireNotNull(metadata.zipUrls.medium),
            frames = metadata.frames.map { UgoiraFrame(it.file, it.delay) },
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun galleryDetail(statusKey: MicroBlogKey): Cacheable<GalleryDetail> =
        postHandler.post(statusKey).map {
            val post = it.contentPostOrNull() ?: error("Gallery detail should be a post")
            post.toGalleryDetail(accountKey)
        } as Cacheable<GalleryDetail>

    override fun galleryComments(statusKey: MicroBlogKey): RemoteLoader<UiTimelineV2> =
        PixivGalleryCommentsLoader(
            service = service,
            accountKey = accountKey,
            statusKey = statusKey,
        )

    override fun galleryRecommendations(statusKey: MicroBlogKey): RemoteLoader<UiTimelineV2> =
        PixivGalleryRecommendationsLoader(
            service = service,
            accountKey = accountKey,
            statusKey = statusKey,
        )

    override fun searchStatus(query: String): RemoteLoader<UiTimelineV2> =
        PixivSearchTimelineLoader(
            service = service,
            accountKey = accountKey,
            query = query,
        )

    override val kagariSearchKinds: List<KagariSearchKind> =
        listOf(KagariSearchKind.Latest, KagariSearchKind.Oldest, KagariSearchKind.Popular, KagariSearchKind.Users)

    override fun kagariSearchStatus(
        query: String,
        kind: KagariSearchKind,
    ): RemoteLoader<UiTimelineV2> =
        PixivSearchTimelineLoader(
            service = service,
            accountKey = accountKey,
            query = query,
            sort =
                when (kind) {
                    KagariSearchKind.Oldest -> PixivSearchSort.DateAsc
                    KagariSearchKind.Popular -> PixivSearchSort.PopularDesc
                    else -> PixivSearchSort.DateDesc
                },
        )

    override fun searchUser(query: String): RemoteLoader<UiProfile> =
        PixivSearchUserLoader(
            service = service,
            accountKey = accountKey,
            query = query,
        )

    override fun discoverUsers(): RemoteLoader<UiProfile> =
        PixivDiscoverUserLoader(
            service = service,
            accountKey = accountKey,
        )

    override fun discoverStatuses(): RemoteLoader<UiTimelineV2> =
        PixivDiscoverTimelineLoader(
            service = service,
            accountKey = accountKey,
        )

    override fun discoverHashtags(): RemoteLoader<UiHashtag> =
        PixivTrendHashtagLoader(
            service = service,
        )

    fun bookmarkTimelineLoader(restrict: PixivRestrict = PixivRestrict.Public): RemoteLoader<UiTimelineV2> =
        PixivBookmarkTimelineLoader(
            service = service,
            credentialFlow = credentialFlow,
            accountKey = accountKey,
            restrict = restrict,
        )

    fun followingTimelineLoader(restrict: PixivFollowRestrict = PixivFollowRestrict.Public): RemoteLoader<UiTimelineV2> =
        PixivFollowingTimelineLoader(
            service = service,
            accountKey = accountKey,
            restrict = restrict,
        )

    fun rankingTimelineLoader(mode: PixivRankingMode): RemoteLoader<UiTimelineV2> =
        PixivRankingTimelineLoader(
            service = service,
            accountKey = accountKey,
            mode = mode,
        )

    override fun following(userKey: MicroBlogKey): RemoteLoader<UiProfile> = notSupported()

    override fun fans(userKey: MicroBlogKey): RemoteLoader<UiProfile> = notSupported()

    override suspend fun profileTabs(userKey: MicroBlogKey): ImmutableList<ProfileTab> =
        persistentListOf(
            ProfileTab(
                name = UiStrings.Illustrations,
                displayType = ProfileTab.DisplayType.Gallery,
                showAllImagesInGallery = false,
                loader =
                    PixivUserTimelineLoader(
                        service = service,
                        accountKey = accountKey,
                        userKey = userKey,
                        type = PixivWorkType.Illust,
                    ),
            ),
            ProfileTab(
                name = UiStrings.Manga,
                displayType = ProfileTab.DisplayType.Gallery,
                showAllImagesInGallery = false,
                loader =
                    PixivUserTimelineLoader(
                        service = service,
                        accountKey = accountKey,
                        userKey = userKey,
                        type = PixivWorkType.Manga,
                    ),
            ),
        )

    override suspend fun handle(
        event: PostEvent,
        updater: DatabaseUpdater,
    ) {
        require(event is PostEvent.Pixiv)
        when (event) {
            is PostEvent.Pixiv.Bookmark -> bookmark(event, updater)
            is PostEvent.Pixiv.Follow -> follow(event, updater)
        }
    }

    private suspend fun bookmark(
        event: PostEvent.Pixiv.Bookmark,
        updater: DatabaseUpdater,
    ) {
        val illustId = event.postKey.pixivIllustId()
        val known = event.state ?: if (event.bookmarked) PixivBookmarkState.Bookmarked else PixivBookmarkState.None
        val detail =
            if (known.isBookmarked) {
                service.bookmarkDetail(illustId).bookmarkDetail
            } else {
                null
            }
        val current = detail?.toBookmarkState() ?: PixivBookmarkState.None
        val target =
            when (event.kind) {
                PixivBookmarkKind.MatchFollow -> followMatchedRestrict(event.authorKey)
                PixivBookmarkKind.Public -> PixivRestrict.Public
                PixivBookmarkKind.Private -> PixivRestrict.Private
            }
        val targetState = target.toBookmarkState()
        if (current == targetState) {
            updateBookmarkCache(updater, event.postKey, event.authorKey, PixivBookmarkState.None, -1)
            service.deleteBookmark(illustId = illustId)
        } else {
            updateBookmarkCache(updater, event.postKey, event.authorKey, targetState, if (current.isBookmarked) 0 else 1)
            service.addBookmark(
                illustId = illustId,
                restrict = target,
                tags = detail?.tags.orEmpty().filter { it.isRegistered }.map { it.name },
            )
        }
    }

    private suspend fun followMatchedRestrict(authorKey: MicroBlogKey?): PixivRestrict {
        if (authorKey == null) return PixivRestrict.Public
        val userId = authorKey.id.toLongOrNull() ?: return PixivRestrict.Public
        val followDetail = service.followDetail(userId).followDetail
        val isPrivate = followDetail.isFollowed && followDetail.restrict == PixivRestrict.Private.value
        PixivPrivateFollows.update(accountKey, authorKey, isPrivate)
        return if (isPrivate) PixivRestrict.Private else PixivRestrict.Public
    }

    private suspend fun fetchPrivateFollows(): Set<MicroBlogKey> {
        val userId = credentialFlow.first().userId
        val users = mutableSetOf<MicroBlogKey>()
        var response = service.userFollowing(userId = userId, restrict = PixivRestrict.Private)
        while (true) {
            response.userPreviews.mapTo(users) { pixivUserKey(it.user.id) }
            val nextUrl = response.nextUrl ?: break
            response = service.nextUsers(nextUrl)
        }
        return users
    }

    private suspend fun follow(
        event: PostEvent.Pixiv.Follow,
        updater: DatabaseUpdater,
    ) {
        val userId = event.userKey.id.toLongOrNull() ?: throw IllegalArgumentException("Invalid Pixiv user id: ${event.userKey.id}")
        val followed = !event.followed
        updater.updateCache(event.postKey) { post ->
            if (post is UiTimelineV2.Post) {
                post.copy(
                    actions =
                        post.actions.withPixivFollowState(
                            statusKey = event.postKey,
                            accountKey = accountKey,
                            userKey = event.userKey,
                            followed = followed,
                        ),
                )
            } else {
                post
            }
        }
        if (followed) {
            service.followUser(
                userId = userId,
                restrict = if (event.isPrivate) PixivRestrict.Private else PixivRestrict.Public,
            )
        } else {
            service.unfollowUser(userId = userId)
        }
        PixivPrivateFollows.update(accountKey, event.userKey, followed && event.isPrivate)
    }

    private suspend fun updateBookmarkCache(
        updater: DatabaseUpdater,
        postKey: MicroBlogKey,
        authorKey: MicroBlogKey?,
        state: PixivBookmarkState,
        countDelta: Long,
    ) {
        updater.updateCache(postKey) { post ->
            if (post is UiTimelineV2.Post) {
                val count = post.actions.pixivBookmarkCount(postKey) ?: 0L
                post.copy(
                    actions =
                        post.actions.withPixivBookmarkState(
                            statusKey = postKey,
                            accountKey = accountKey,
                            authorKey = authorKey ?: post.user?.key,
                            state = state,
                            count = (count + countDelta).coerceAtLeast(0),
                        ),
                )
            } else {
                post
            }
        }
    }

    override suspend fun kagariPixivBookmarkDetail(statusKey: MicroBlogKey): KagariPixivBookmarkDetail {
        val detail = service.bookmarkDetail(statusKey.pixivIllustId()).bookmarkDetail
        return KagariPixivBookmarkDetail(
            isBookmarked = detail.isBookmarked,
            isPrivate = detail.isBookmarked && detail.restrict == PixivRestrict.Private.value,
            tags = detail.tags.map { KagariPixivBookmarkTag(name = it.name, isRegistered = it.isRegistered) },
        )
    }

    override suspend fun kagariPixivSaveBookmark(
        statusKey: MicroBlogKey,
        isPrivate: Boolean,
        tags: List<String>,
    ) {
        val illustId = statusKey.pixivIllustId()
        val wasBookmarked = service.bookmarkDetail(illustId).bookmarkDetail.isBookmarked
        val restrict = if (isPrivate) PixivRestrict.Private else PixivRestrict.Public
        service.addBookmark(
            illustId = illustId,
            restrict = restrict,
            tags = tags,
        )
        updateBookmarkCache(postEventHandler, statusKey, null, restrict.toBookmarkState(), if (wasBookmarked) 0 else 1)
    }

    override suspend fun kagariPixivRemoveBookmark(statusKey: MicroBlogKey) {
        service.deleteBookmark(illustId = statusKey.pixivIllustId())
        updateBookmarkCache(postEventHandler, statusKey, null, PixivBookmarkState.None, -1)
    }

    private fun MicroBlogKey.pixivIllustId(): Long = id.toLongOrNull() ?: throw IllegalArgumentException("Invalid Pixiv illust id: $id")

    private fun PixivRestrict.toBookmarkState(): PixivBookmarkState =
        when (this) {
            PixivRestrict.Public -> PixivBookmarkState.Public
            PixivRestrict.Private -> PixivBookmarkState.Private
        }
}

private val pixivLogo: IconType = IconType.Logo(UiIcon.Pixiv)

private val rankingIcon: IconType = UiIcon.KagariTrophy.withPixivLogo()

private fun UiIcon.withPixivLogo(): IconType = IconType.Badged(IconType.Material(this), pixivLogo)
