package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.PostInteractionSettingsPref
import app.bsky.actor.PostInteractionSettingsPrefPostgateEmbeddingRuleUnion
import app.bsky.actor.PostInteractionSettingsPrefThreadgateAllowRuleUnion
import app.bsky.actor.PreferencesUnion
import app.bsky.feed.Postgate
import app.bsky.feed.PostgateDisableRule
import app.bsky.feed.PostgateEmbeddingRuleUnion
import app.bsky.feed.Threadgate
import app.bsky.feed.ThreadgateAllowUnion
import app.bsky.feed.ThreadgateFollowerRule
import app.bsky.feed.ThreadgateFollowingRule
import app.bsky.feed.ThreadgateListRule
import app.bsky.feed.ThreadgateMentionRule
import app.bsky.feed.GetPostsQueryParams
import app.bsky.graph.GetListsQueryParams
import com.atproto.repo.ApplyWritesCreate
import com.atproto.repo.ApplyWritesRequest
import com.atproto.repo.ApplyWritesRequestWriteUnion
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.GetRecordQueryParams
import dev.dimension.flare.data.datasource.microblog.InteractionGate
import dev.dimension.flare.data.datasource.microblog.InteractionGateConfig
import dev.dimension.flare.data.datasource.microblog.InteractionGateList
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.JsonContent
import kotlin.random.Random
import kotlin.time.Clock

private val kagariGateDefaults = KagariAccountCache<InteractionGate>()

internal fun blueskyInteractionGateConfig(
    accountKey: MicroBlogKey,
    getService: suspend () -> BlueskyService,
): InteractionGateConfig =
    InteractionGateConfig(
        loadDefault = {
            (
                getService()
                    .kagariPreferences<PreferencesUnion.PostInteractionSettingsPref>()
                    .firstOrNull()
                    ?.value
                    ?.toInteractionGate()
                    ?: InteractionGate()
            ).also { kagariGateDefaults.put(accountKey, it) }
        },
        cachedDefault = { kagariGateDefaults[accountKey] },
        saveDefault = { gate ->
            getService().kagariReplacePreference(PreferencesUnion.PostInteractionSettingsPref(gate.toPref()))
            kagariGateDefaults.put(accountKey, gate)
        },
        loadLists = {
            getService()
                .getLists(
                    params =
                        GetListsQueryParams(
                            actor = Did(did = accountKey.id),
                            limit = 100,
                        ),
                ).requireResponse()
                .lists
                .filter { it.purpose == app.bsky.graph.Token.Curatelist }
                .map { InteractionGateList(uri = it.uri.atUri, name = it.name) }
        },
    )

internal fun InteractionGate?.needsBlueskyRecords(): Boolean =
    this != null && (reply.toThreadgateAllow() != null || !allowQuote)

internal suspend fun createBlueskyPostWithGate(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    post: JsonContent,
    gate: InteractionGate,
) {
    val tid = newTid()
    val rkey = RKey(tid)
    val postUri = AtUri("at://${accountKey.id}/app.bsky.feed.post/$tid")
    val now = Clock.System.now()
    val writes =
        buildList {
            add(create("app.bsky.feed.post", rkey, post))
            gate.reply.toThreadgateAllow()?.let { allow ->
                add(
                    create(
                        "app.bsky.feed.threadgate",
                        rkey,
                        Threadgate(post = postUri, allow = allow, createdAt = now).bskyJson(),
                    ),
                )
            }
            if (!gate.allowQuote) {
                add(
                    create(
                        "app.bsky.feed.postgate",
                        rkey,
                        Postgate(
                            createdAt = now,
                            post = postUri,
                            embeddingRules = listOf(PostgateEmbeddingRuleUnion.DisableRule(PostgateDisableRule)),
                        ).bskyJson(),
                    ),
                )
            }
        }
    service
        .applyWrites(
            request =
                ApplyWritesRequest(
                    repo = Did(did = accountKey.id),
                    writes = writes.toImmutableList(),
                ),
        ).requireResponse()
}

internal data class BlueskyStoredGate(
    val gate: InteractionGate,
    val isReply: Boolean,
    val hiddenReplies: List<AtUri>,
    val detached: List<AtUri>,
)

internal suspend fun loadBlueskyInteractionGate(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    postUri: String,
): BlueskyStoredGate {
    val post =
        service
            .getRecord(
                params =
                    GetRecordQueryParams(
                        repo = Did(did = accountKey.id),
                        collection = Nsid("app.bsky.feed.post"),
                        rkey = RKey(postUri.substringAfterLast('/')),
                    ),
            ).requireResponse()
            .value
    val isReply = post.kagariIsReply()
    val threadgate = loadThreadgate(service, accountKey, postUri)
    val postgate = loadPostgate(service, accountKey, postUri)
    val allow = threadgate?.allow
    val reply =
        when {
            threadgate == null || allow == null -> InteractionGate.Reply.Anyone
            allow.isEmpty() -> InteractionGate.Reply.Nobody
            else ->
                InteractionGate.Reply.Some(
                    followers = allow.any { it is ThreadgateAllowUnion.FollowerRule },
                    following = allow.any { it is ThreadgateAllowUnion.FollowingRule },
                    mentioned = allow.any { it is ThreadgateAllowUnion.MentionRule },
                    lists = allow.filterIsInstance<ThreadgateAllowUnion.ListRule>().map { it.value.list.atUri },
                )
        }
    val allowQuote = postgate?.embeddingRules.orEmpty().none { it is PostgateEmbeddingRuleUnion.DisableRule }
    return BlueskyStoredGate(
        gate = InteractionGate(reply = reply, allowQuote = allowQuote),
        isReply = isReply,
        hiddenReplies = threadgate?.hiddenReplies.orEmpty(),
        detached = postgate?.detachedEmbeddingUris.orEmpty(),
    )
}

internal suspend fun saveBlueskyInteractionGate(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    postUri: String,
    gate: InteractionGate,
) {
    val stored = loadBlueskyInteractionGate(service, accountKey, postUri)
    val rkey = RKey(postUri.substringAfterLast('/'))
    val repo = Did(did = accountKey.id)
    val uri = AtUri(postUri)
    val now = Clock.System.now()
    if (!stored.isReply) {
        val allow = gate.reply.toThreadgateAllow()
        service.writeGateRecord(
            repo = repo,
            collection = THREADGATE_COLLECTION,
            rkey = rkey,
            record =
                if (allow == null && stored.hiddenReplies.isEmpty()) {
                    null
                } else {
                    Threadgate(
                        post = uri,
                        allow = allow,
                        createdAt = now,
                        hiddenReplies = stored.hiddenReplies.ifEmpty { null },
                    ).bskyJson()
                },
        )
    }
    service.writeGateRecord(
        repo = repo,
        collection = POSTGATE_COLLECTION,
        rkey = rkey,
        record =
            if (gate.allowQuote && stored.detached.isEmpty()) {
                null
            } else {
                Postgate(
                    createdAt = now,
                    post = uri,
                    detachedEmbeddingUris = stored.detached.ifEmpty { null },
                    embeddingRules =
                        if (gate.allowQuote) {
                            null
                        } else {
                            listOf(PostgateEmbeddingRuleUnion.DisableRule(PostgateDisableRule))
                        },
                ).bskyJson()
            },
    )
}

internal suspend fun setBlueskyQuoteDetached(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    quotedPostUri: String,
    quotingPostUri: String,
    detach: Boolean,
) {
    val rkey = RKey(quotedPostUri.substringAfterLast('/'))
    val repo = Did(did = accountKey.id)
    val existing = loadPostgate(service, accountKey, quotedPostUri)
    val target = AtUri(quotingPostUri)
    val others = existing?.detachedEmbeddingUris.orEmpty().filterNot { it == target }
    val detached = if (detach) others + target else others
    val rules = existing?.embeddingRules
    service.writeGateRecord(
        repo = repo,
        collection = POSTGATE_COLLECTION,
        rkey = rkey,
        record =
            if (detached.isEmpty() && rules.isNullOrEmpty()) {
                null
            } else {
                Postgate(
                    createdAt = existing?.createdAt ?: Clock.System.now(),
                    post = AtUri(quotedPostUri),
                    detachedEmbeddingUris = detached.ifEmpty { null },
                    embeddingRules = rules,
                ).bskyJson()
            },
    )
}

internal class KagariTooManyHiddenRepliesException : IllegalStateException("too many hidden replies")

internal data class BlueskyReplyVisibility(
    val rootUri: String,
    val hidden: Boolean,
)

internal suspend fun loadBlueskyReplyVisibility(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    replyUri: String,
): BlueskyReplyVisibility {
    val reply =
        service
            .getPosts(GetPostsQueryParams(persistentListOf(AtUri(replyUri))))
            .requireResponse()
            .posts
            .first()
    val rootUri = reply.record.kagariReplyRootUri() ?: error("not a reply")
    val threadgate = loadThreadgate(service, accountKey, rootUri)
    return BlueskyReplyVisibility(
        rootUri = rootUri,
        hidden = threadgate?.hiddenReplies.orEmpty().any { it.atUri == replyUri },
    )
}

internal suspend fun setBlueskyReplyHidden(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    replyUri: String,
    hide: Boolean,
) {
    val visibility = loadBlueskyReplyVisibility(service, accountKey, replyUri)
    val rootUri = visibility.rootUri
    val current = loadThreadgate(service, accountKey, rootUri)
    val hidden = current?.hiddenReplies.orEmpty().filterNot { it.atUri == replyUri }
    val next = if (hide) hidden + AtUri(replyUri) else hidden
    if (next.size > MAX_HIDDEN_REPLIES) throw KagariTooManyHiddenRepliesException()
    val repo = Did(did = accountKey.id)
    val rkey = RKey(rootUri.substringAfterLast('/'))
    service.writeGateRecord(
        repo = repo,
        collection = THREADGATE_COLLECTION,
        rkey = rkey,
        record =
            if (current?.allow == null && next.isEmpty()) {
                null
            } else {
                Threadgate(
                    post = AtUri(rootUri),
                    allow = current?.allow,
                    createdAt = current?.createdAt ?: Clock.System.now(),
                    hiddenReplies = next.ifEmpty { null },
                ).bskyJson()
            },
    )
}

private const val MAX_HIDDEN_REPLIES = 300

private const val THREADGATE_COLLECTION = "app.bsky.feed.threadgate"
private const val POSTGATE_COLLECTION = "app.bsky.feed.postgate"

private suspend fun loadThreadgate(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    postUri: String,
): Threadgate? =
    service
        .kagariGetRecord(Did(did = accountKey.id), THREADGATE_COLLECTION, RKey(postUri.substringAfterLast('/')))
        ?.let { runCatching { it.decodeAs<Threadgate>() }.getOrNull() }

private suspend fun loadPostgate(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    postUri: String,
): Postgate? =
    service
        .kagariGetRecord(Did(did = accountKey.id), POSTGATE_COLLECTION, RKey(postUri.substringAfterLast('/')))
        ?.let { runCatching { it.decodeAs<Postgate>() }.getOrNull() }

private suspend fun BlueskyService.writeGateRecord(
    repo: Did,
    collection: String,
    rkey: RKey,
    record: JsonContent?,
) {
    if (record == null) {
        deleteRecord(DeleteRecordRequest(repo = repo, collection = Nsid(collection), rkey = rkey))
    } else {
        kagariPutRecord(repo, collection, rkey, record)
    }
}

private fun create(
    collection: String,
    rkey: RKey,
    value: JsonContent,
): ApplyWritesRequestWriteUnion =
    ApplyWritesRequestWriteUnion.Create(
        value =
            ApplyWritesCreate(
                collection = Nsid(collection),
                rkey = rkey,
                value = value,
            ),
    )

private const val TID_CHARS = "234567abcdefghijklmnopqrstuvwxyz"

private fun newTid(): String {
    val now = Clock.System.now()
    val micros = now.epochSeconds * 1_000_000 + now.nanosecondsOfSecond / 1_000
    var value = (micros shl 10) or Random.nextLong(0, 1024)
    val out = CharArray(13)
    for (i in 12 downTo 0) {
        out[i] = TID_CHARS[(value and 31).toInt()]
        value = value ushr 5
    }
    return out.concatToString()
}

private fun InteractionGate.Reply.toThreadgateAllow(): List<ThreadgateAllowUnion>? =
    when (this) {
        InteractionGate.Reply.Anyone -> null
        InteractionGate.Reply.Nobody -> emptyList()
        is InteractionGate.Reply.Some ->
            if (isEmpty) {
                null
            } else {
                buildList {
                    if (mentioned) add(ThreadgateAllowUnion.MentionRule(ThreadgateMentionRule))
                    if (following) add(ThreadgateAllowUnion.FollowingRule(ThreadgateFollowingRule))
                    if (followers) add(ThreadgateAllowUnion.FollowerRule(ThreadgateFollowerRule))
                    lists.forEach { add(ThreadgateAllowUnion.ListRule(ThreadgateListRule(list = AtUri(it)))) }
                }.take(InteractionGate.MAX_RULES)
            }
    }

private fun PostInteractionSettingsPref.toInteractionGate(): InteractionGate {
    val rules = threadgateAllowRules
    val reply =
        when {
            rules == null -> InteractionGate.Reply.Anyone
            rules.isEmpty() -> InteractionGate.Reply.Nobody
            else ->
                InteractionGate.Reply.Some(
                    followers = rules.any { it is PostInteractionSettingsPrefThreadgateAllowRuleUnion.FollowerRule },
                    following = rules.any { it is PostInteractionSettingsPrefThreadgateAllowRuleUnion.FollowingRule },
                    mentioned = rules.any { it is PostInteractionSettingsPrefThreadgateAllowRuleUnion.MentionRule },
                    lists =
                        rules
                            .filterIsInstance<PostInteractionSettingsPrefThreadgateAllowRuleUnion.ListRule>()
                            .map { it.value.list.atUri },
                )
        }
    val allowQuote =
        postgateEmbeddingRules.orEmpty().none {
            it is PostInteractionSettingsPrefPostgateEmbeddingRuleUnion.DisableRule
        }
    return InteractionGate(reply = reply, allowQuote = allowQuote)
}

private fun InteractionGate.toPref(): PostInteractionSettingsPref {
    val rules: List<PostInteractionSettingsPrefThreadgateAllowRuleUnion>? =
        when (val reply = reply) {
            InteractionGate.Reply.Anyone -> null
            InteractionGate.Reply.Nobody -> emptyList()
            is InteractionGate.Reply.Some ->
                if (reply.isEmpty) {
                    null
                } else {
                    buildList {
                        if (reply.mentioned) {
                            add(PostInteractionSettingsPrefThreadgateAllowRuleUnion.MentionRule(ThreadgateMentionRule))
                        }
                        if (reply.following) {
                            add(PostInteractionSettingsPrefThreadgateAllowRuleUnion.FollowingRule(ThreadgateFollowingRule))
                        }
                        if (reply.followers) {
                            add(PostInteractionSettingsPrefThreadgateAllowRuleUnion.FollowerRule(ThreadgateFollowerRule))
                        }
                        reply.lists.forEach {
                            add(
                                PostInteractionSettingsPrefThreadgateAllowRuleUnion.ListRule(
                                    ThreadgateListRule(list = AtUri(it)),
                                ),
                            )
                        }
                    }.take(InteractionGate.MAX_RULES)
                }
        }
    val embedding: List<PostInteractionSettingsPrefPostgateEmbeddingRuleUnion> =
        if (allowQuote) {
            emptyList()
        } else {
            listOf(PostInteractionSettingsPrefPostgateEmbeddingRuleUnion.DisableRule(PostgateDisableRule))
        }
    return PostInteractionSettingsPref(
        threadgateAllowRules = rules?.toImmutableList(),
        postgateEmbeddingRules = embedding.toImmutableList(),
    )
}
