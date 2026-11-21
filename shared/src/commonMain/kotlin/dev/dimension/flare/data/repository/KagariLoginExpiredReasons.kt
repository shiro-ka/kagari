@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package dev.dimension.flare.data.repository

import dev.dimension.flare.model.MicroBlogKey
import kotlin.concurrent.atomics.AtomicReference

public object KagariLoginExpiredReasons {
    private val reasons = AtomicReference<Map<MicroBlogKey, String>>(emptyMap())

    public fun record(
        accountKey: MicroBlogKey,
        reason: String,
    ) {
        while (true) {
            val current = reasons.load()
            if (reasons.compareAndSet(current, current + (accountKey to reason))) return
        }
    }

    public fun get(accountKey: MicroBlogKey): String? = reasons.load()[accountKey]
}
