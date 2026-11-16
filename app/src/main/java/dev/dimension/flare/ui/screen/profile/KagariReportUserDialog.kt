package dev.dimension.flare.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariReportUserPresenter
import dev.dimension.flare.ui.presenter.profile.KagariReportUserState
import dev.dimension.flare.ui.presenter.status.action.BlueskyReportStatusState
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariReportUserDialog(
    accountType: AccountType,
    userKey: MicroBlogKey,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariReportUserPresenter_${accountType}_$userKey") {
        kagariReportUserPresenter(accountType, userKey)
    }
    if (!state.supported) {
        ReportUserDialog(accountType = accountType, userKey = userKey, onBack = onBack)
        return
    }
    AlertDialog(
        onDismissRequest = onBack,
        confirmButton = {
            TextButton(
                enabled = state.reason != null,
                onClick = {
                    state.reason?.let {
                        state.report(it)
                        onBack.invoke()
                    }
                },
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.report_user_title))
        },
        text = {
            Column {
                Text(text = stringResource(id = R.string.report_user_description))
                state.allReasons.forEach {
                    val interactionSource = remember(it) { MutableInteractionSource() }
                    ListItem(
                        modifier =
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { state.selectReason(it) },
                            ),
                        leadingContent = {
                            RadioButton(
                                selected = state.reason == it,
                                interactionSource = interactionSource,
                                onClick = { state.selectReason(it) },
                            )
                        },
                        supportingContent = {
                            Text(text = stringResource(id = it.kagariDescriptionRes))
                        },
                        elevation = ListItemDefaults.elevation(),
                        content = {
                            Text(text = stringResource(id = it.kagariTitleRes))
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun kagariReportUserPresenter(
    accountType: AccountType,
    userKey: MicroBlogKey,
) = run {
    val state =
        remember(accountType, userKey) {
            KagariReportUserPresenter(accountType = accountType, userKey = userKey)
        }.invoke()
    object : KagariReportUserState by state {
    }
}

private val BlueskyReportStatusState.ReportReason.kagariTitleRes: Int
    get() =
        when (this) {
            BlueskyReportStatusState.ReportReason.Spam -> R.string.report_reason_spam_title
            BlueskyReportStatusState.ReportReason.Violation -> R.string.report_reason_violation_title
            BlueskyReportStatusState.ReportReason.Misleading -> R.string.report_reason_misleading_title
            BlueskyReportStatusState.ReportReason.Sexual -> R.string.report_reason_sexual_title
            BlueskyReportStatusState.ReportReason.Rude -> R.string.report_reason_rude_title
            BlueskyReportStatusState.ReportReason.Other -> R.string.report_reason_other_title
        }

private val BlueskyReportStatusState.ReportReason.kagariDescriptionRes: Int
    get() =
        when (this) {
            BlueskyReportStatusState.ReportReason.Spam -> R.string.report_reason_spam_description
            BlueskyReportStatusState.ReportReason.Violation -> R.string.report_reason_violation_description
            BlueskyReportStatusState.ReportReason.Misleading -> R.string.report_reason_misleading_description
            BlueskyReportStatusState.ReportReason.Sexual -> R.string.report_reason_sexual_description
            BlueskyReportStatusState.ReportReason.Rude -> R.string.report_reason_rude_description
            BlueskyReportStatusState.ReportReason.Other -> R.string.report_reason_other_description
        }
