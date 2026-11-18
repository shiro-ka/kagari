package dev.dimension.flare.ui.presenter.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.atproto.admin.RepoRef
import com.atproto.moderation.CreateReportRequest
import com.atproto.moderation.CreateReportRequestSubjectUnion
import com.atproto.moderation.Token
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.rememberKagariBluesky
import dev.dimension.flare.ui.presenter.status.action.BlueskyReportStatusState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sh.christian.ozone.api.Did

public class KagariReportUserPresenter(
    private val accountType: AccountType,
    private val userKey: MicroBlogKey,
) : PresenterBase<KagariReportUserState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariReportUserState {
        var reason by remember { mutableStateOf<BlueskyReportStatusState.ReportReason?>(null) }
        val bluesky = accountService.rememberKagariBluesky(accountType)
        return object : KagariReportUserState {
            override val supported: Boolean = bluesky != null
            override val allReasons = BlueskyReportStatusState.ReportReason.entries.toImmutableList()
            override val reason: BlueskyReportStatusState.ReportReason?
                get() = reason

            override fun selectReason(value: BlueskyReportStatusState.ReportReason) {
                reason = value
            }

            override fun report(value: BlueskyReportStatusState.ReportReason) {
                val dataSource = bluesky ?: return
                scope.launch {
                    dataSource.kagariReportUser(userKey, value)
                }
            }
        }
    }
}

@Immutable
public interface KagariReportUserState {
    public val supported: Boolean
    public val reason: BlueskyReportStatusState.ReportReason?
    public val allReasons: ImmutableList<BlueskyReportStatusState.ReportReason>

    public fun selectReason(value: BlueskyReportStatusState.ReportReason)

    public fun report(value: BlueskyReportStatusState.ReportReason)
}

internal suspend fun BlueskyDataSource.kagariReportUser(
    userKey: MicroBlogKey,
    reason: BlueskyReportStatusState.ReportReason,
) {
    runCatching {
        kagariService().createReport(
            CreateReportRequest(
                reasonType =
                    when (reason) {
                        BlueskyReportStatusState.ReportReason.Spam -> Token.ReasonSpam
                        BlueskyReportStatusState.ReportReason.Violation -> Token.ReasonViolation
                        BlueskyReportStatusState.ReportReason.Misleading -> Token.ReasonMisleading
                        BlueskyReportStatusState.ReportReason.Sexual -> Token.ReasonSexual
                        BlueskyReportStatusState.ReportReason.Rude -> Token.ReasonRude
                        BlueskyReportStatusState.ReportReason.Other -> Token.ReasonOther
                    },
                subject =
                    CreateReportRequestSubjectUnion.AdminRepoRef(
                        value = RepoRef(did = Did(did = userKey.id)),
                    ),
            ),
        )
    }
}
