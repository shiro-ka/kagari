package dev.dimension.flare.ui.presenter.dm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.paging.compose.collectAsLazyPagingItems
import dev.dimension.flare.common.PagingState
import dev.dimension.flare.common.toPagingState
import dev.dimension.flare.data.datasource.microblog.capabilities
import dev.dimension.flare.data.repository.AccountRepository
import dev.dimension.flare.data.repository.accountServiceProvider
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.UiDMRoom
import dev.dimension.flare.ui.model.map
import dev.dimension.flare.ui.presenter.PresenterBase

public class KagariDMRequestsPresenter(
    private val accountType: AccountType,
) : PresenterBase<PagingState<UiDMRoom>>() {
    private val accountRepository: AccountRepository by koinInject()

    @Composable
    override fun body(): PagingState<UiDMRoom> {
        val scope = rememberCoroutineScope()
        val serviceState = accountServiceProvider(accountType = accountType, repository = accountRepository)
        val directMessageState =
            remember(serviceState) {
                serviceState.map { requireNotNull(it.capabilities.directMessage) }
            }
        return directMessageState
            .map { directMessage ->
                remember(directMessage) {
                    directMessage.directMessageHandler.kagariRequests(scope)
                }.collectAsLazyPagingItems()
            }.toPagingState()
    }
}
