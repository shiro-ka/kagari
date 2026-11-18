package dev.dimension.flare.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal fun kagariToast(
    context: Context,
    @StringRes message: Int,
) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

internal fun kagariToast(
    context: Context,
    message: String,
) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
internal fun KagariToggleAction(
    ready: Boolean,
    on: Boolean,
    @StringRes onDone: Int,
    @StringRes offDone: Int,
    @StringRes failed: Int,
    apply: (Boolean, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        apply(on) { ok ->
            val message =
                when {
                    !ok -> failed
                    on -> onDone
                    else -> offDone
                }
            kagariToast(context, message)
            onBack()
        }
    }
}

@Composable
internal fun KagariLoadingDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = if (title != null) { { Text(title) } } else null,
        text = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
    )
}

@Composable
internal fun KagariMessageDialog(
    message: String,
    onDismissRequest: () -> Unit,
    title: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = if (title != null) { { Text(title) } } else null,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(android.R.string.ok)) }
        },
    )
}

@Composable
internal fun KagariConfirmDialog(
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    title: String? = null,
    text: String? = null,
    confirmEnabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = if (title != null) { { Text(title) } } else null,
        text = if (text != null) { { Text(text) } } else null,
        confirmButton = {
            TextButton(enabled = confirmEnabled, onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
internal fun KagariLoadingBox(padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun KagariMessageBox(
    message: String,
    padding: PaddingValues,
) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(text = message)
    }
}
