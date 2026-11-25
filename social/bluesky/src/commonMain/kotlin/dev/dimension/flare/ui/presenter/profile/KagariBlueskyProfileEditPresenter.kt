package dev.dimension.flare.ui.presenter.profile

import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.datasource.bluesky.KagariBlueskyProfileEditor
import dev.dimension.flare.data.datasource.bluesky.KagariProfileDraft
import dev.dimension.flare.data.datasource.microblog.KagariProfileEditor
import dev.dimension.flare.data.datasource.microblog.MicroblogDataSource
import dev.dimension.flare.model.MicroBlogKey

public class KagariBlueskyProfileEditPresenter(
    accountKey: MicroBlogKey,
) : KagariProfileEditPresenterBase<KagariProfileDraft>(accountKey) {
    override fun editorOf(dataSource: MicroblogDataSource): KagariProfileEditor<KagariProfileDraft>? =
        (dataSource as? BlueskyDataSource)?.let(::KagariBlueskyProfileEditor)
}
