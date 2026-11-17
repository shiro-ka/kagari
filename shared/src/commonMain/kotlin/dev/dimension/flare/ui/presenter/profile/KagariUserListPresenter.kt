package dev.dimension.flare.ui.presenter.profile

import dev.dimension.flare.data.datasource.microblog.MicroblogDataSource
import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.UiProfile

public class KagariUserListPresenter(
    accountType: AccountType,
    private val loader: (MicroblogDataSource) -> RemoteLoader<UiProfile>,
) : UserListPresenter(accountType) {
    override fun dataSource(service: MicroblogDataSource): RemoteLoader<UiProfile> = loader(service)
}
