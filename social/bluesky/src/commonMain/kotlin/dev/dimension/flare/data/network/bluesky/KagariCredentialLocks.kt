@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package dev.dimension.flare.data.network.bluesky

import dev.dimension.flare.model.MicroBlogKey
import kotlinx.coroutines.sync.Mutex
import kotlin.concurrent.atomics.AtomicReference

internal object KagariCredentialLocks {
    private val locks = AtomicReference<Map<MicroBlogKey, Mutex>>(emptyMap())

    fun forAccount(accountKey: MicroBlogKey?): Mutex {
        if (accountKey == null) return Mutex()
        while (true) {
            val current = locks.load()
            current[accountKey]?.let { return it }
            val created = Mutex()
            if (locks.compareAndSet(current, current + (accountKey to created))) return created
        }
    }
}
