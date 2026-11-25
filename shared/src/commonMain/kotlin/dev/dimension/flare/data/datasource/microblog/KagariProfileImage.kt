package dev.dimension.flare.data.datasource.microblog

public class KagariProfileImage(
    public val bytes: ByteArray,
    public val mimeType: String,
)

public data class KagariProfileImageChanges(
    val avatar: KagariProfileImage? = null,
    val banner: KagariProfileImage? = null,
    val removeBanner: Boolean = false,
) {
    public val isEmpty: Boolean get() = avatar == null && banner == null && !removeBanner
}

public interface KagariProfileDraftImages {
    public val avatarUrl: String?
    public val bannerUrl: String?
}

public interface KagariProfileEditor<D : KagariProfileDraftImages> {
    public suspend fun load(): D

    public suspend fun save(
        before: D,
        after: D,
        images: KagariProfileImageChanges,
    )
}
