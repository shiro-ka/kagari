package dev.dimension.flare.ui.model.mapper

import app.bsky.richtext.Facet
import app.bsky.richtext.FacetByteSlice
import app.bsky.richtext.FacetFeatureUnion
import dev.dimension.flare.data.network.bluesky.normalizeHttpUrl
import moe.tlaster.twitter.parser.HashTagToken
import moe.tlaster.twitter.parser.UrlToken
import moe.tlaster.twitter.parser.UserNameToken

private const val KAGARI_SHORT_URL_MAX_PATH = 15
private const val KAGARI_SHORT_URL_KEEP_PATH = 13

internal fun kagariBlueskyHighlights(text: String): List<IntRange> {
    if (text.isEmpty()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var index = 0
    for (token in parser.parse(text)) {
        val start = index
        val end = index + token.value.length
        val highlighted =
            when (token) {
                is UrlToken -> normalizeHttpUrl(token.value) != null
                is HashTagToken -> true
                is UserNameToken -> true
                else -> false
            }
        if (highlighted && end > start && end <= text.length) {
            ranges.add(start until end)
        }
        index = end
    }
    return ranges
}

internal fun kagariBlueskyFirstLink(text: String): String? =
    parser
        .parse(text)
        .asSequence()
        .filterIsInstance<UrlToken>()
        .firstNotNullOfOrNull { normalizeHttpUrl(it.value) }

internal data class KagariShortenedPost(
    val text: String,
    val facets: List<Facet>,
)

internal fun kagariShortenBlueskyLinks(
    text: String,
    facets: List<Facet>,
): KagariShortenedPost {
    if (facets.none { facet -> facet.features.any { it is FacetFeatureUnion.Link } }) {
        return KagariShortenedPost(text, facets)
    }
    val bytes = text.encodeToByteArray()
    val sorted = facets.sortedBy { it.index.byteStart }
    val output = mutableListOf<Byte>()
    val result = mutableListOf<Facet>()
    var cursor = 0
    var delta = 0L
    for (facet in sorted) {
        val start = facet.index.byteStart.toInt()
        val end = facet.index.byteEnd.toInt()
        if (start < cursor || end < start || end > bytes.size) {
            result.add(facet.shifted(delta))
            continue
        }
        val isLink = facet.features.any { it is FacetFeatureUnion.Link }
        if (!isLink) {
            result.add(facet.shifted(delta))
            continue
        }
        val original = bytes.copyOfRange(start, end).decodeToString()
        val shortened = kagariToShortUrl(original).encodeToByteArray()
        for (i in cursor until start) output.add(bytes[i])
        val newStart = output.size.toLong()
        shortened.forEach { output.add(it) }
        result.add(
            Facet(
                index = FacetByteSlice(byteStart = newStart, byteEnd = newStart + shortened.size),
                features = facet.features,
            ),
        )
        delta += shortened.size - (end - start)
        cursor = end
    }
    for (i in cursor until bytes.size) output.add(bytes[i])
    return KagariShortenedPost(
        text = output.toByteArray().decodeToString(),
        facets = result.sortedBy { it.index.byteStart },
    )
}

private fun Facet.shifted(delta: Long): Facet =
    if (delta == 0L) {
        this
    } else {
        Facet(
            index = FacetByteSlice(byteStart = index.byteStart + delta, byteEnd = index.byteEnd + delta),
            features = features,
        )
    }

internal fun kagariToShortUrl(url: String): String {
    val schemeEnd = url.indexOf("://")
    if (schemeEnd <= 0) return url
    val scheme = url.substring(0, schemeEnd).lowercase()
    if (scheme != "http" && scheme != "https") return url
    val rest = url.substring(schemeEnd + 3)
    val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }.let { if (it < 0) rest.length else it }
    val authority = rest.substring(0, authorityEnd).substringAfterLast('@')
    if (authority.isEmpty()) return url
    val host = authority.withoutDefaultPort(scheme).lowercase()
    val tail = rest.substring(authorityEnd)
    val hashIndex = tail.indexOf('#')
    val beforeHash = if (hashIndex < 0) tail else tail.substring(0, hashIndex)
    val hash = if (hashIndex < 0) "" else tail.substring(hashIndex)
    val queryIndex = beforeHash.indexOf('?')
    val pathname = (if (queryIndex < 0) beforeHash else beforeHash.substring(0, queryIndex)).ifEmpty { "/" }
    val search = if (queryIndex < 0) "" else beforeHash.substring(queryIndex)
    val path =
        (if (pathname == "/") "" else pathname).kagariPercentEncoded() +
            search.takeIf { it.length > 1 }.orEmpty().kagariPercentEncoded() +
            hash.takeIf { it.length > 1 }.orEmpty().kagariPercentEncoded()
    return if (path.length > KAGARI_SHORT_URL_MAX_PATH) {
        host + path.take(KAGARI_SHORT_URL_KEEP_PATH) + "..."
    } else {
        host + path
    }
}

private fun String.withoutDefaultPort(scheme: String): String {
    if (startsWith("[")) return this
    val colon = lastIndexOf(':')
    if (colon < 0) return this
    val port = substring(colon + 1)
    val isDefault = (scheme == "http" && port == "80") || (scheme == "https" && port == "443") || port.isEmpty()
    return if (isDefault) substring(0, colon) else this
}

private fun Char.isKagariUrlSafe(): Boolean = code in 0x21..0x7E && this != '"' && this != '<' && this != '>' && this != '`'

private fun String.kagariPercentEncoded(): String {
    if (all { it.isKagariUrlSafe() }) return this
    val builder = StringBuilder()
    var i = 0
    while (i < length) {
        val char = this[i]
        val size = if (char.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) 2 else 1
        if (size == 1 && char.isKagariUrlSafe()) {
            builder.append(char)
        } else {
            substring(i, i + size).encodeToByteArray().forEach { byte ->
                builder.append('%')
                builder.append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
            }
        }
        i += size
    }
    return builder.toString()
}
