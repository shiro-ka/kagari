package dev.dimension.flare.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import io.github.composefluent.FluentTheme
import io.github.kdroidfilter.nucleus.window.DecoratedWindow
import io.github.kdroidfilter.nucleus.window.DecoratedWindowScope
import io.github.kdroidfilter.nucleus.window.TitleBar
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import org.apache.commons.lang3.SystemUtils

@Composable
internal fun FrameWindowScope.PlatformTitleBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit = {},
) {
    if (this is DecoratedWindowScope) {
        TitleBar(
            style =
                LocalTitleBarStyle.current.copy(
                    colors =
                        LocalTitleBarStyle.current.colors.copy(
                            background =
                                FluentTheme.colors.background.mica.base
                                    .copy(alpha = 0f),
                            inactiveBackground = Color.Transparent,
                        ),
                ),
            modifier = modifier,
            content = {
                Row(
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    content()
                }
            },
        )
    } else {
        Row(
            modifier =
                modifier
                    .padding(
                        start = 72.dp,
                    ),
        ) {
            content()
        }
    }
}

@Composable
internal fun PlatformWindow(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "",
    icon: Painter? = null,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit,
) {
    if (SystemUtils.IS_OS_WINDOWS) {
        DecoratedWindow(
            onCloseRequest = onCloseRequest,
            state = state,
            visible = visible,
            title = title,
            icon = icon,
            resizable = resizable,
            enabled = enabled,
            focusable = focusable,
            alwaysOnTop = alwaysOnTop,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
            content = content,
        )
    } else {
        Window(
            onCloseRequest = onCloseRequest,
            state = state,
            visible = visible,
            title = title,
            icon = icon,
            resizable = resizable,
            enabled = enabled,
            focusable = focusable,
            alwaysOnTop = alwaysOnTop,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
            content = content,
        )
    }
}
