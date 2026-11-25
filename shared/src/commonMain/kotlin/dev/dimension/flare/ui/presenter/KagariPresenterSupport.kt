package dev.dimension.flare.ui.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.dimension.flare.data.datasource.microblog.MicroblogDataSource
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.collectAsUiState
import dev.dimension.flare.ui.model.takeSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
public fun AccountService.rememberKagariService(accountType: AccountType): MicroblogDataSource? {
    val flow = remember(accountType) { accountServiceFlow(accountType) }
    val service by flow.collectAsUiState()
    return service.takeSuccess()
}

public suspend fun <T : Any> kagariLoadState(block: suspend () -> T): UiState<T> =
    runCatching { block() }
        .fold(
            onSuccess = { UiState.Success(it) },
            onFailure = { UiState.Error(it) },
        )

public fun <T> CoroutineScope.kagariLaunch(
    busy: ((Boolean) -> Unit)? = null,
    onDone: (Result<T>) -> Unit,
    action: suspend () -> T,
) {
    busy?.invoke(true)
    launch {
        val result = runCatching { action() }
        withContext(Dispatchers.Main) {
            busy?.invoke(false)
            onDone(result)
        }
    }
}

public fun CoroutineScope.kagariSubmit(
    busy: ((Boolean) -> Unit)? = null,
    onResult: (Boolean) -> Unit,
    action: suspend () -> Unit,
) {
    kagariLaunch(busy = busy, onDone = { onResult(it.isSuccess) }, action = action)
}
