package dev.dimension.flare.ui.screen.profile

import androidx.compose.runtime.Composable
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.screen.status.action.KagariReportDialog

@Composable
internal fun KagariReportUserDialog(
    accountType: AccountType?,
    userKey: MicroBlogKey,
    onBack: () -> Unit,
) {
    if (accountType == null) {
        ReportUserDialog(accountType = null, userKey = userKey, onBack = onBack)
        return
    }
    KagariReportDialog(
        accountType = accountType,
        userKey = userKey,
        onBack = onBack,
        fallback = { ReportUserDialog(accountType = accountType, userKey = userKey, onBack = onBack) },
    )
}
