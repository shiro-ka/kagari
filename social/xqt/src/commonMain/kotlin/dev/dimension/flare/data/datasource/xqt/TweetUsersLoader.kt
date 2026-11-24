package dev.dimension.flare.data.datasource.xqt

import de.jensklingenberg.ktorfit.Response
import dev.dimension.flare.common.encodeJson
import dev.dimension.flare.data.database.cache.mapper.nextPageCursor
import dev.dimension.flare.data.database.cache.mapper.users
import dev.dimension.flare.data.datasource.microblog.paging.PagingRequest
import dev.dimension.flare.data.datasource.microblog.paging.PagingResult
import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
import dev.dimension.flare.data.network.xqt.api.UserListApi
import dev.dimension.flare.data.network.xqt.model.Error
import dev.dimension.flare.data.network.xqt.model.InstructionUnion
import dev.dimension.flare.data.network.xqt.model.TimelineV2
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.model.mapper.render
import dev.dimension.flare.ui.render.toUi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlin.time.Instant

internal enum class TweetUsers(
    val label: String,
) {
    RepostedBy("reposts"),
    LikedBy("likes"),
}

internal class TweetUsersLoader(
    private val service: UserListApi,
    private val accountKey: MicroBlogKey,
    private val statusKey: MicroBlogKey,
    private val kind: TweetUsers,
) : RemoteLoader<UiTimelineV2> {
    override suspend fun load(
        pageSize: Int,
        request: PagingRequest,
    ): PagingResult<UiTimelineV2> {
        var cursor =
            when (request) {
                PagingRequest.Refresh -> null
                is PagingRequest.Prepend -> return PagingResult()
                is PagingRequest.Append -> request.nextKey
            }
        val visited = mutableSetOf<String?>()
        while (true) {
            visited += cursor
            val instructions = instructions(TweetUsersVariables(statusKey.id, pageSize.coerceIn(1, 100), cursor).encodeJson())
            val users =
                instructions.users().map { user ->
                    val profile = user.render(accountKey)
                    UiTimelineV2.User(
                        value = profile,
                        createdAt = Instant.DISTANT_PAST.toUi(),
                        statusKey = profile.key,
                        accountType = AccountType.Specific(accountKey),
                    )
                }
            val next = instructions.nextPageCursor(cursor)?.takeUnless { it in visited }
            if (users.isNotEmpty() || next == null) return PagingResult(data = users, nextKey = next)
            cursor = next
        }
    }

    private suspend fun instructions(variables: String): List<InstructionUnion> =
        when (kind) {
            TweetUsers.RepostedBy -> {
                service.getRetweeters(variables = variables).instructions { it.data.retweetersTimeline to it.errors }
            }

            TweetUsers.LikedBy -> {
                service.getFavoriters(variables = variables).instructions { it.data.favoritersTimeline to it.errors }
            }
        }

    private inline fun <T> Response<T>.instructions(read: (T) -> Pair<TimelineV2, List<Error>?>): List<InstructionUnion> {
        check(isSuccessful) { "Could not load X ${kind.label} (HTTP $code)" }
        val (timeline, errors) = read(checkNotNull(body()) { "X ${kind.label} did not return a user list" })
        check(errors.isNullOrEmpty()) { "Could not load X ${kind.label}: ${errors?.firstOrNull()?.message}" }
        return timeline.timeline.instructions
    }
}

@Serializable
private data class TweetUsersVariables(
    val tweetId: String,
    val count: Int,
    val cursor: String? = null,
    @Required
    val includePromotedContent: Boolean = false,
)
