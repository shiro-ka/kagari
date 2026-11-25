package dev.dimension.flare.ui.presenter

import androidx.compose.runtime.Composable
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.model.AccountType

@Composable
internal fun AccountService.rememberKagariBluesky(accountType: AccountType): BlueskyDataSource? =
    rememberKagariService(accountType) as? BlueskyDataSource
