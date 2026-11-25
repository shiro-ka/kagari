package dev.dimension.flare.ui.presenter.profile

import dev.dimension.flare.data.datasource.microblog.KagariProfileEditor
import dev.dimension.flare.data.datasource.microblog.MicroblogDataSource
import dev.dimension.flare.data.datasource.xqt.KagariTwitterProfileDraft
import dev.dimension.flare.data.datasource.xqt.XQTDataSource
import dev.dimension.flare.model.MicroBlogKey

public class KagariTwitterProfileEditPresenter(
    accountKey: MicroBlogKey,
) : KagariProfileEditPresenterBase<KagariTwitterProfileDraft>(accountKey) {
    override fun editorOf(dataSource: MicroblogDataSource): KagariProfileEditor<KagariTwitterProfileDraft>? =
        (dataSource as? XQTDataSource)?.kagariProfileEditor
}
