package dev.dimension.flare.data.datasource.microblog.handler

import dev.dimension.flare.common.Cacheable
import dev.dimension.flare.common.decodeProtobuf
import dev.dimension.flare.data.database.cache.CacheDatabase
import dev.dimension.flare.data.database.cache.model.DbEmoji
import dev.dimension.flare.data.database.cache.model.EmojiContent
import dev.dimension.flare.data.datasource.microblog.loader.EmojiLoader
import dev.dimension.flare.di.koinGet
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.ui.model.UiEmoji
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.SerializationException
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public class EmojiHandler(
    private val host: String,
    private val loader: EmojiLoader,
) {
    private val database: CacheDatabase by koinInject()
    public val emoji: Cacheable<ImmutableMap<String, ImmutableList<UiEmoji>>> =
        Cacheable(
            fetchSource = {
                val emojis = loader.emojis()
                database.emojiDao().insert(
                    DbEmoji(
                        host = host,
                        content = EmojiContent(emojis),
                    ),
                )
            },
            cacheSource = {
                cachedEmojis(database, host)
            },
        )

    public companion object {
        public fun cached(host: String): Flow<ImmutableMap<String, ImmutableList<UiEmoji>>> = cachedEmojis(koinGet(), host)
    }
}

private fun cachedEmojis(
    database: CacheDatabase,
    host: String,
): Flow<ImmutableMap<String, ImmutableList<UiEmoji>>> =
    database
        .emojiDao()
        .getContent(host)
        .distinctUntilChanged()
        .transform { content ->
            if (content == null) {
                return@transform
            }
            val decoded =
                runCatching {
                    if (content.isEmpty()) {
                        EmojiContent()
                    } else {
                        content.decodeProtobuf<EmojiContent>()
                    }
                }.getOrElse { throwable ->
                    if (throwable is SerializationException) {
                        database.emojiDao().delete(host)
                        return@transform
                    }
                    throw throwable
                }
            emit(decoded.data)
        }
