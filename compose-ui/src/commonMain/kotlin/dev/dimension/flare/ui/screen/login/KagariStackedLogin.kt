package dev.dimension.flare.ui.screen.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_login_app_password
import dev.dimension.flare.compose.ui.kagari_login_oauth
import dev.dimension.flare.compose.ui.mastodon_login_verify_message
import dev.dimension.flare.data.datasource.bluesky.kagariIsBluesky
import dev.dimension.flare.ui.component.platform.KagariPrimaryButton
import dev.dimension.flare.ui.component.platform.PlatformFilledTonalButton
import dev.dimension.flare.ui.component.platform.PlatformLinearProgressIndicator
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.component.resolveText
import dev.dimension.flare.ui.presenter.login.LoginCookieSnapshot
import dev.dimension.flare.ui.presenter.login.LoginEffect
import dev.dimension.flare.ui.presenter.login.LoginFlowPresenter
import dev.dimension.flare.ui.presenter.login.LoginMethodSpec
import dev.dimension.flare.ui.presenter.login.LoginMethodType
import moe.tlaster.precompose.molecule.producePresenter
import org.jetbrains.compose.resources.stringResource

private const val KAGARI_LOGIN_ACTION = "login"

internal fun kagariUsesStackedLogin(
    platformId: String,
    methods: List<LoginMethodSpec>,
): Boolean =
    kagariIsBluesky(platformId) &&
        methods.any { it.type == LoginMethodType.OAuth } &&
        methods.any { it.type == LoginMethodType.Password }

@Composable
internal fun KagariStackedLoginContent(
    state: SelectionPresenter.State,
    platformId: String,
    host: String,
    openUri: (String) -> Unit,
    onWebViewLogin: (
        request: LoginEffect.OpenWebCookieLogin,
        cookieCallback: suspend (LoginCookieSnapshot) -> Boolean,
    ) -> Unit,
    registerDeeplinkCallback: @Composable ((url: String) -> Boolean) -> Unit,
) {
    val oauthHandler =
        remember(platformId, host) {
            state.createLoginHandler(platformId = platformId, host = host, methodType = LoginMethodType.OAuth)
        }
    val passwordHandler =
        remember(platformId, host) {
            state.createLoginHandler(platformId = platformId, host = host, methodType = LoginMethodType.Password)
        }
    val oauthState by producePresenter("kagari_login_oauth_${platformId}_$host") {
        remember(oauthHandler) { LoginFlowPresenter(oauthHandler) }.body()
    }
    val passwordState by producePresenter("kagari_login_password_${platformId}_$host") {
        remember(passwordHandler) { LoginFlowPresenter(passwordHandler) }.body()
    }
    registerDeeplinkCallback { url ->
        when {
            oauthState.canResume(url) -> {
                oauthState.resume(url)
                true
            }

            passwordState.canResume(url) -> {
                passwordState.resume(url)
                true
            }

            else -> false
        }
    }
    KagariLoginEffects(oauthState, openUri, onWebViewLogin)
    KagariLoginEffects(passwordState, openUri, onWebViewLogin)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        val oauthFlow = oauthState.flowState
        oauthFlow.actions.fastForEach { action ->
            val enabled = action.enabled && !oauthFlow.loading
            if (action.id == KAGARI_LOGIN_ACTION) {
                KagariPrimaryButton(
                    onClick = { oauthState.perform(action.id) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = enabled,
                ) {
                    PlatformText(text = stringResource(Res.string.kagari_login_oauth))
                }
            } else {
                PlatformFilledTonalButton(
                    onClick = { oauthState.perform(action.id) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = enabled,
                ) {
                    PlatformText(text = action.label.resolveText())
                }
            }
        }
        KagariLoginStatus(oauthState)
        Spacer(modifier = Modifier.height(28.dp))
        val passwordFlow = passwordState.flowState
        passwordFlow.fields.fastForEach { field ->
            LoginFieldInput(
                field = field,
                enabled = !passwordFlow.loading && !field.readOnly,
                onUpdate = passwordState::updateField,
                onSubmit = {
                    passwordFlow.actions.firstOrNull { it.enabled }?.let { passwordState.perform(it.id) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        passwordFlow.actions.fastForEach { action ->
            PlatformFilledTonalButton(
                onClick = { passwordState.perform(action.id) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = action.enabled && !passwordFlow.loading,
            ) {
                PlatformText(
                    text =
                        if (action.id == KAGARI_LOGIN_ACTION) {
                            stringResource(Res.string.kagari_login_app_password)
                        } else {
                            action.label.resolveText()
                        },
                )
            }
        }
        KagariLoginStatus(passwordState)
    }
}

@Composable
private fun KagariLoginEffects(
    loginState: LoginFlowPresenter.State,
    openUri: (String) -> Unit,
    onWebViewLogin: (
        request: LoginEffect.OpenWebCookieLogin,
        cookieCallback: suspend (LoginCookieSnapshot) -> Boolean,
    ) -> Unit,
) {
    LaunchedEffect(loginState.effects) {
        loginState.effects.collect { effect ->
            when (effect) {
                is LoginEffect.OpenUrl -> openUri(effect.url)
                is LoginEffect.OpenWebCookieLogin -> onWebViewLogin(effect, loginState::checkCookies)
                is LoginEffect.ShowQr -> Unit
            }
        }
    }
}

@Composable
private fun KagariLoginStatus(loginState: LoginFlowPresenter.State) {
    val flow = loginState.flowState
    flow.error?.let {
        PlatformText(text = it, textAlign = TextAlign.Center)
    }
    if (flow.loading) {
        PlatformText(
            text = stringResource(Res.string.mastodon_login_verify_message),
            textAlign = TextAlign.Center,
        )
        PlatformLinearProgressIndicator()
    }
}
