package dev.dimension.flare.ui.model.mapper

import dev.dimension.flare.data.datasource.microblog.handler.EmojiHandler
import dev.dimension.flare.di.koinGet
import dev.dimension.flare.ui.model.UiEmoji
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal object MisskeyLocalEmojis {
    private val urls = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    private val observedHosts = MutableStateFlow<Set<String>>(emptySet())

    fun url(
        host: String,
        name: String,
    ): String? {
        observe(host)
        return urls.value[host]?.get(name)
    }

    fun update(
        host: String,
        emojis: Map<String, List<UiEmoji>>,
    ) {
        val byName =
            emojis.values
                .asSequence()
                .flatten()
                .filter { it.url.isNotEmpty() }
                .associate { it.shortcode.trim(':') to it.url }
        urls.update { it + (host to byName) }
    }

    private fun observe(host: String) {
        val current = observedHosts.value
        if (host in current || !observedHosts.compareAndSet(current, current + host)) {
            return
        }
        runCatching {
            val scope = koinGet<CoroutineScope>()
            EmojiHandler
                .cached(host)
                .onEach { update(host, it) }
                .catch { }
                .launchIn(scope)
        }
    }
}
