package dev.dimension.flare.data.datasource.xqt

import com.fleeksoft.ksoup.parser.Parser
import dev.dimension.flare.common.UploadMedia
import dev.dimension.flare.data.datasource.microblog.KagariProfileDraftImages
import dev.dimension.flare.data.datasource.microblog.KagariProfileEditor
import dev.dimension.flare.data.datasource.microblog.KagariProfileImage
import dev.dimension.flare.data.datasource.microblog.KagariProfileImageChanges
import dev.dimension.flare.data.network.xqt.XQTMediaUploader
import dev.dimension.flare.data.network.xqt.XQTService
import dev.dimension.flare.data.network.xqt.api.kagariRemoveProfileBanner
import dev.dimension.flare.data.network.xqt.api.kagariUpdateProfile
import dev.dimension.flare.data.network.xqt.api.kagariUpdateProfileBanner
import dev.dimension.flare.data.network.xqt.api.kagariUpdateProfileImage
import dev.dimension.flare.data.network.xqt.model.User
import dev.dimension.flare.data.network.xqt.model.UserUnavailable
import dev.dimension.flare.data.network.xqt.model.XqtUrl
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.mapper.avatarUrl
import dev.dimension.flare.ui.model.mapper.name

public data class KagariTwitterProfileDraft(
    val name: String,
    val description: String,
    val location: String,
    val url: String,
    override val avatarUrl: String?,
    override val bannerUrl: String?,
) : KagariProfileDraftImages

internal class KagariTwitterProfileEditor(
    private val service: XQTService,
    private val accountKey: MicroBlogKey,
) : KagariProfileEditor<KagariTwitterProfileDraft> {
    override suspend fun load(): KagariTwitterProfileDraft {
        val user =
            when (val result = service.userById(accountKey.id).body()?.data?.user?.result) {
                is User -> result
                is UserUnavailable, null -> error("X could not load your profile")
            }
        return user.kagariProfileDraft()
    }

    override suspend fun save(
        before: KagariTwitterProfileDraft,
        after: KagariTwitterProfileDraft,
        images: KagariProfileImageChanges,
    ) {
        if (after.copy(avatarUrl = before.avatarUrl, bannerUrl = before.bannerUrl) != before) {
            service.kagariUpdateProfile(
                name = after.name,
                description = after.description,
                location = after.location,
                url = after.url,
            )
        }
        images.avatar?.let { service.kagariUpdateProfileImage(upload(it, "avatar")) }
        val banner = images.banner
        if (banner != null) {
            service.kagariUpdateProfileBanner(upload(banner, "banner"))
        } else if (images.removeBanner) {
            service.kagariRemoveProfileBanner()
        }
    }

    private suspend fun upload(
        image: KagariProfileImage,
        name: String,
    ): String = XQTMediaUploader(service).upload(UploadMedia.fromBytes(name = name, bytes = image.bytes, mimeType = image.mimeType))
}

internal fun User.kagariProfileDraft(): KagariTwitterProfileDraft =
    KagariTwitterProfileDraft(
        name = name.kagariDecodeEntities(),
        description =
            legacy.description
                .orEmpty()
                .kagariExpandUrls(legacy.entities?.description?.urls)
                .kagariDecodeEntities(),
        location = (legacy.location ?: location?.location).orEmpty().kagariDecodeEntities(),
        url =
            legacy.url
                ?.let { short ->
                    legacy.entities
                        ?.url
                        ?.urls
                        ?.firstOrNull { it.url == short }
                        ?.expandedUrl ?: short
                }.orEmpty(),
        avatarUrl = avatarUrl,
        bannerUrl = legacy.profileBannerUrl,
    )

private fun String.kagariExpandUrls(urls: List<XqtUrl>?): String =
    urls.orEmpty().fold(this) { text, url ->
        val expanded = url.expandedUrl
        if (expanded.isNullOrEmpty()) text else text.replace(url.url, expanded)
    }

private fun String.kagariDecodeEntities(): String = Parser.unescapeEntities(this, inAttribute = false)
