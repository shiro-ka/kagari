package dev.dimension.flare.ui.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimension.flare.data.datasource.bluesky.kagariReportLabelers
import dev.dimension.flare.data.datasource.bluesky.kagariSendReport
import dev.dimension.flare.data.datasource.microblog.KagariReportLabeler
import dev.dimension.flare.data.datasource.microblog.KagariReportReason
import dev.dimension.flare.data.datasource.microblog.kagariReportCandidates
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.map
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope

public class KagariReportPresenter(
    private val accountType: AccountType,
    private val statusKey: MicroBlogKey? = null,
    private val userKey: MicroBlogKey? = null,
) : PresenterBase<KagariReportState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariReportState {
        val bluesky = accountService.rememberKagariBluesky(accountType)
        var labelers by remember { mutableStateOf<UiState<List<KagariReportLabeler>>>(UiState.Loading()) }
        var sending by remember { mutableStateOf(false) }
        LaunchedEffect(bluesky) {
            val source = bluesky ?: return@LaunchedEffect
            labelers = kagariLoadState { source.kagariReportLabelers() }
        }
        val isAccount = statusKey == null
        return object : KagariReportState {
            override val supported: Boolean = bluesky != null
            override val isAccount: Boolean = isAccount
            override val sending: Boolean = sending

            override fun candidates(reason: KagariReportReason): UiState<ImmutableList<KagariReportLabeler>> =
                labelers.map { it.kagariReportCandidates(reason, isAccount).toImmutableList() }

            override fun send(
                labeler: KagariReportLabeler,
                reason: KagariReportReason,
                details: String,
                onResult: (Boolean) -> Unit,
            ) {
                val source = bluesky ?: return onResult(false)
                scope.kagariSubmit(busy = { sending = it }, onResult = onResult) {
                    source.kagariSendReport(
                        labeler = labeler,
                        reason = reason,
                        details = details,
                        statusKey = statusKey,
                        userKey = userKey,
                    )
                }
            }
        }
    }
}

@Immutable
public interface KagariReportState {
    public val supported: Boolean
    public val isAccount: Boolean
    public val sending: Boolean

    public fun candidates(reason: KagariReportReason): UiState<ImmutableList<KagariReportLabeler>>

    public fun send(
        labeler: KagariReportLabeler,
        reason: KagariReportReason,
        details: String,
        onResult: (Boolean) -> Unit,
    )
}
