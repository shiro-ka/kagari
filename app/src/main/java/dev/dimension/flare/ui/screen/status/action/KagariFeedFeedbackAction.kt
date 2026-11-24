package dev.dimension.flare.ui.screen.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariFeedFeedbackPresenter
import dev.dimension.flare.ui.screen.KagariToggleAction
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariFeedFeedbackAction(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    feedUri: String,
    feedServiceDid: String,
    feedContext: String?,
    reqId: String?,
    more: Boolean,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariFeedFeedbackPresenter_${accountType}_${statusKey}_$more") {
        remember(accountType, statusKey, feedUri) {
            KagariFeedFeedbackPresenter(
                accountType = accountType,
                feedUri = feedUri,
                feedServiceDid = feedServiceDid,
                postUri = statusKey.id,
                feedContext = feedContext,
                reqId = reqId,
            )
        }.invoke()
    }
    KagariToggleAction(
        ready = state.ready,
        on = more,
        onDone = R.string.kagari_feed_feedback_sent,
        offDone = R.string.kagari_feed_feedback_sent,
        failed = R.string.kagari_feed_feedback_failed,
        apply = { value, onResult -> state.send(value, onResult) },
        onBack = onBack,
    )
}
