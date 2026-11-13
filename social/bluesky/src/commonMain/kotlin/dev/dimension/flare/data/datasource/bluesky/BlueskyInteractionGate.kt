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
import com.atproto.repo.CreateRecordRequest
import dev.dimension.flare.data.datasource.microblog.InteractionGate
import dev.dimension.flare.data.datasource.microblog.InteractionGateConfig
import dev.dimension.flare.data.datasource.microblog.InteractionGateList
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.collections.immutable.toImmutableList
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
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

internal suspend fun applyBlueskyInteractionGate(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    postUri: AtUri,
    gate: InteractionGate?,
) {
    if (gate == null) return
    val rkey = RKey(postUri.atUri.substringAfterLast('/'))
    val allow = gate.reply.toThreadgateAllow()
    if (allow != null) {
        service
            .createRecord(
                CreateRecordRequest(
                    repo = Did(did = accountKey.id),
                    collection = Nsid("app.bsky.feed.threadgate"),
                    rkey = rkey,
                    record =
                        Threadgate(
                            post = postUri,
                            allow = allow,
                            createdAt = Clock.System.now(),
                        ).bskyJson(),
                ),
            ).requireResponse()
    }
    if (!gate.allowQuote) {
        service
            .createRecord(
                CreateRecordRequest(
                    repo = Did(did = accountKey.id),
                    collection = Nsid("app.bsky.feed.postgate"),
                    rkey = rkey,
                    record =
                        Postgate(
                            createdAt = Clock.System.now(),
                            post = postUri,
                            embeddingRules =
                                listOf(PostgateEmbeddingRuleUnion.DisableRule(PostgateDisableRule)),
                        ).bskyJson(),
                ),
            ).requireResponse()
    }
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
