package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.PreferencesUnion
import app.bsky.actor.ProfileView
import app.bsky.feed.GetPostsQueryParams
import app.bsky.labeler.GetServicesQueryParams
import app.bsky.labeler.GetServicesResponseViewUnion
import com.atproto.admin.RepoRef
import com.atproto.moderation.CreateReportRequest
import com.atproto.moderation.CreateReportRequestSubjectUnion
import com.atproto.moderation.Token
import com.atproto.repo.StrongRef
import dev.dimension.flare.data.datasource.microblog.KAGARI_BLUESKY_MODERATION_DID
import dev.dimension.flare.data.datasource.microblog.KagariReportLabeler
import dev.dimension.flare.data.datasource.microblog.KagariReportReason
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

internal suspend fun BlueskyDataSource.kagariReportLabelers(): List<KagariReportLabeler> {
    val service = kagariService()
    val subscribed =
        runCatching {
            service
                .kagariPreferences<PreferencesUnion.LabelersPref>()
                .flatMap { pref -> pref.value.labelers.map { it.did.did } }
        }.getOrDefault(emptyList())
    val dids = (listOf(KAGARI_BLUESKY_MODERATION_DID) + subscribed).distinct()
    val labelers =
        service
            .getServices(
                GetServicesQueryParams(
                    dids = dids.map { Did(it) }.toImmutableList(),
                    detailed = true,
                ),
            ).requireResponse()
            .views
            .mapNotNull { view ->
                when (view) {
                    is GetServicesResponseViewUnion.LabelerViewDetailed ->
                        kagariReportLabeler(
                            creator = view.value.creator,
                            reasonTypes = view.value.reasonTypes?.map { it.value },
                            subjectTypes = view.value.subjectTypes?.map { it.value },
                            subjectCollections = view.value.subjectCollections?.map { it.nsid },
                        )

                    is GetServicesResponseViewUnion.LabelerView ->
                        kagariReportLabeler(
                            creator = view.value.creator,
                            reasonTypes = null,
                            subjectTypes = null,
                            subjectCollections = null,
                        )

                    else -> null
                }
            }
    return labelers.sortedBy { dids.indexOf(it.did) }
}

private fun kagariReportLabeler(
    creator: ProfileView,
    reasonTypes: List<String>?,
    subjectTypes: List<String>?,
    subjectCollections: List<String>?,
): KagariReportLabeler =
    KagariReportLabeler(
        did = creator.did.did,
        name = creator.displayName?.takeIf { it.isNotBlank() } ?: creator.handle.handle,
        handle = creator.handle.handle,
        avatar = creator.avatar?.uri,
        reasonTypes = reasonTypes,
        subjectTypes = subjectTypes,
        subjectCollections = subjectCollections,
    )

internal suspend fun BlueskyDataSource.kagariSendReport(
    labeler: KagariReportLabeler,
    reason: KagariReportReason,
    details: String?,
    statusKey: MicroBlogKey?,
    userKey: MicroBlogKey?,
) {
    val service = kagariService()
    val subject =
        if (statusKey != null) {
            val post =
                service
                    .getPosts(GetPostsQueryParams(persistentListOf(AtUri(statusKey.id))))
                    .requireResponse()
                    .posts
                    .firstOrNull()
                    ?: error("post not found")
            CreateReportRequestSubjectUnion.RepoStrongRef(
                value = StrongRef(uri = post.uri, cid = post.cid),
            )
        } else {
            CreateReportRequestSubjectUnion.AdminRepoRef(
                value = RepoRef(did = Did(did = checkNotNull(userKey).id)),
            )
        }
    service
        .kagariForLabeler(labeler.did)
        .createReport(
            CreateReportRequest(
                reasonType = Token.safeValueOf(labeler.reasonTypeFor(reason)),
                reason = details?.trim()?.takeIf { it.isNotEmpty() },
                subject = subject,
            ),
        ).requireResponse()
}
