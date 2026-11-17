package dev.dimension.flare.ui.presenter.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.bsky.actor.GetProfileQueryParams
import dev.dimension.flare.data.datasource.bluesky.BlueskyDataSource
import dev.dimension.flare.data.repository.AccountService
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.collectAsUiState
import dev.dimension.flare.ui.model.mapper.render
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.PresenterBase
import dev.dimension.flare.ui.render.UiDateTime
import dev.dimension.flare.ui.render.toUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sh.christian.ozone.api.Did

public class KagariVerificationPresenter(
    private val accountType: AccountType,
    private val userKey: MicroBlogKey,
) : PresenterBase<KagariVerificationState>() {
    private val accountService: AccountService by koinInject()

    @Composable
    override fun body(): KagariVerificationState {
        val flow = remember(accountType) { accountService.accountServiceFlow(accountType) }
        val service by flow.collectAsUiState()
        val dataSource = service.takeSuccess()
        var verifiers by remember { mutableStateOf<UiState<ImmutableList<KagariVerifier>>>(UiState.Loading()) }
        LaunchedEffect(dataSource, userKey) {
            if (dataSource == null) return@LaunchedEffect
            verifiers =
                if (dataSource is BlueskyDataSource) {
                    runCatching { dataSource.kagariVerifiers(userKey) }
                        .fold(
                            onSuccess = { UiState.Success(it) },
                            onFailure = { UiState.Error(it) },
                        )
                } else {
                    UiState.Success(persistentListOf())
                }
        }
        return object : KagariVerificationState {
            override val verifiers: UiState<ImmutableList<KagariVerifier>> = verifiers
        }
    }
}

@Immutable
public interface KagariVerificationState {
    public val verifiers: UiState<ImmutableList<KagariVerifier>>
}

@Immutable
public data class KagariVerifier(
    val profile: UiProfile,
    val createdAt: UiDateTime,
    val trustedVerifier: Boolean,
)

internal suspend fun BlueskyDataSource.kagariVerifiers(userKey: MicroBlogKey): ImmutableList<KagariVerifier> {
    val service = kagariService()
    val target =
        service
            .getProfile(GetProfileQueryParams(actor = Did(did = userKey.id)))
            .requireResponse()
    val views =
        target.verification
            ?.verifications
            .orEmpty()
            .filter { it.isValid }
    return views
        .mapNotNull { view ->
            runCatching {
                service
                    .getProfile(GetProfileQueryParams(actor = view.issuer))
                    .requireResponse()
            }.getOrNull()?.let { issuer ->
                KagariVerifier(
                    profile = issuer.render(accountKey),
                    createdAt = view.createdAt.toUi(),
                    trustedVerifier = issuer.verification?.trustedVerifierStatus?.value == "valid",
                )
            }
        }.toImmutableList()
}
