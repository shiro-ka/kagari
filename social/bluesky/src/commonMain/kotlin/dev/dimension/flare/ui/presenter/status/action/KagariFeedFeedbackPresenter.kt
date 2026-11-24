package dev.dimension.flare.ui.presenter.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.dimension.flare.data.datasource.bluesky.kagariSendFeedFeedback
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.presenter.kagariSubmit
import dev.dimension.flare.ui.presenter.rememberKagariBluesky
import kotlinx.coroutines.CoroutineScope

public class KagariFeedFeedbackPresenter(
    private val accountType: AccountType,
    private val feedUri: String,
    private val feedServiceDid: String,
    private val postUri: String,
    private val feedContext: String?,
    private val reqId: String?,
) : PresenterBase<KagariFeedFeedbackState>() {
    private val scope by koinInject<CoroutineScope>()
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariFeedFeedbackState {
        val dataSource = accountService.rememberKagariBluesky(accountType)
        return object : KagariFeedFeedbackState {
            override val ready: Boolean = dataSource != null

            override fun send(
                more: Boolean,
                onResult: (Boolean) -> Unit,
            ) {
                val source = dataSource ?: return onResult(false)
                scope.kagariSubmit(onResult = onResult) {
                    source.kagariService().kagariSendFeedFeedback(
                        feedUri = feedUri,
                        feedServiceDid = feedServiceDid,
                        postUri = postUri,
                        feedContext = feedContext,
                        reqId = reqId,
                        more = more,
                    )
                }
            }
        }
    }
}

@Immutable
public interface KagariFeedFeedbackState {
    public val ready: Boolean

    public fun send(
        more: Boolean,
        onResult: (Boolean) -> Unit,
    )
}
