package dev.dimension.flare.ui.model.mapper

import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public object PixivPrivateFollows {
    private val scope: CoroutineScope by koinInject()
    private val fetchers = MutableStateFlow<Map<MicroBlogKey, suspend () -> Set<MicroBlogKey>>>(emptyMap())
    private val requested = MutableStateFlow<Set<MicroBlogKey>>(emptySet())
    private val following = MutableStateFlow<Map<MicroBlogKey, Set<MicroBlogKey>>>(emptyMap())

    public val state: StateFlow<Map<MicroBlogKey, Set<MicroBlogKey>>> = following.asStateFlow()

    public fun register(
        accountKey: MicroBlogKey,
        fetch: suspend () -> Set<MicroBlogKey>,
    ) {
        fetchers.update { it + (accountKey to fetch) }
    }

    public fun request(accountKey: MicroBlogKey) {
        val fetch = fetchers.value[accountKey] ?: return
        var started = false
        requested.update { current ->
            started = accountKey !in current
            current + accountKey
        }
        if (!started) return
        scope.launch {
            runCatching { fetch() }
                .onSuccess { users -> following.update { it + (accountKey to users) } }
        }
    }

    public fun update(
        accountKey: MicroBlogKey,
        userKey: MicroBlogKey,
        isPrivate: Boolean,
    ) {
        following.update { current ->
            val users = current[accountKey] ?: return@update current
            val next = if (isPrivate) users + userKey else users - userKey
            if (next == users) current else current + (accountKey to next)
        }
    }
}
