package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.PostInteractionSettingsPref
import app.bsky.actor.PostInteractionSettingsPrefPostgateEmbeddingRuleUnion
import app.bsky.actor.PostInteractionSettingsPrefThreadgateAllowRuleUnion
import app.bsky.actor.PreferencesUnion
import app.bsky.actor.PutPreferencesRequest
import app.bsky.feed.Postgate
import app.bsky.feed.PostgateDisableRule
import app.bsky.feed.PostgateEmbeddingRuleUnion
import app.bsky.feed.Threadgate
import app.bsky.feed.ThreadgateAllowUnion
import app.bsky.feed.ThreadgateFollowerRule
import app.bsky.feed.ThreadgateFollowingRule
import app.bsky.feed.ThreadgateListRule
import app.bsky.feed.ThreadgateMentionRule
import app.bsky.graph.GetListsQueryParams
import com.atproto.repo.ApplyWritesCreate
import com.atproto.repo.ApplyWritesRequest
import com.atproto.repo.ApplyWritesRequestWriteUnion
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.PutRecordRequest
import dev.dimension.flare.data.datasource.microblog.InteractionGate
import dev.dimension.flare.data.datasource.microblog.InteractionGateConfig
import dev.dimension.flare.data.datasource.microblog.InteractionGateList
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.JsonObject
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.JsonContent
import kotlin.random.Random
import kotlin.time.Clock

internal fun blueskyInteractionGateConfig(
    accountKey: MicroBlogKey,
    getService: suspend () -> BlueskyService,
): InteractionGateConfig =
    InteractionGateConfig(
        loadDefault = {
            getService()
                .getPreferences()
                .requireResponse()
                .preferences
                .filterIsInstance<PreferencesUnion.PostInteractionSettingsPref>()
                .firstOrNull()
                ?.value
                ?.toInteractionGate()
                ?: InteractionGate()
        },
        saveDefault = { gate ->
            val service = getService()
            val current =
                service
                    .getPreferences()
                    .requireResponse()
                    .preferences
                    .filterNot { it is PreferencesUnion.PostInteractionSettingsPref }
            service.putPreferences(
                request =
                    PutPreferencesRequest(
                        preferences =
                            (current + PreferencesUnion.PostInteractionSettingsPref(gate.toPref()))
                                .toImmutableList(),
                    ),
            )
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
    val rkey = RKey(postUri.substringAfterLast('/'))
    val post =
        service
            .getRecord(
                params = GetRecordQueryParams(repo = Did(did = accountKey.id), collection = Nsid("app.bsky.feed.post"), rkey = rkey),
            ).requireResponse()
            .value
    val isReply = runCatching { post.decodeAs<JsonObject>()["reply"] != null }.getOrDefault(false)
    val threadgate =
        service
            .getRecord(
                params = GetRecordQueryParams(repo = Did(did = accountKey.id), collection = Nsid("app.bsky.feed.threadgate"), rkey = rkey),
            ).maybeResponse()
            ?.value
            ?.let { runCatching { it.decodeAs<Threadgate>() }.getOrNull() }
    val postgate =
        service
            .getRecord(
                params = GetRecordQueryParams(repo = Did(did = accountKey.id), collection = Nsid("app.bsky.feed.postgate"), rkey = rkey),
            ).maybeResponse()
            ?.value
            ?.let { runCatching { it.decodeAs<Postgate>() }.getOrNull() }
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
        if (allow == null && stored.hiddenReplies.isEmpty()) {
            service.deleteRecord(DeleteRecordRequest(repo = repo, collection = Nsid("app.bsky.feed.threadgate"), rkey = rkey))
        } else {
            service
                .putRecord(
                    PutRecordRequest(
                        repo = repo,
                        collection = Nsid("app.bsky.feed.threadgate"),
                        rkey = rkey,
                        record =
                            Threadgate(
                                post = uri,
                                allow = allow,
                                createdAt = now,
                                hiddenReplies = stored.hiddenReplies.ifEmpty { null },
                            ).bskyJson(),
                    ),
                ).requireResponse()
        }
    }
    if (gate.allowQuote && stored.detached.isEmpty()) {
        service.deleteRecord(DeleteRecordRequest(repo = repo, collection = Nsid("app.bsky.feed.postgate"), rkey = rkey))
    } else {
        service
            .putRecord(
                PutRecordRequest(
                    repo = repo,
                    collection = Nsid("app.bsky.feed.postgate"),
                    rkey = rkey,
                    record =
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
                        ).bskyJson(),
                ),
            ).requireResponse()
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
