package dev.dimension.flare.ui.screen.dm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R

private val kagariQuickReactions = listOf("❤️", "👍", "😆", "👀", "😢")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariDMMessageSheet(
    canReact: Boolean,
    canCopy: Boolean,
    onReact: (String) -> Unit,
    onCopy: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showInput by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            if (canReact) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    kagariQuickReactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clickable { onReact(emoji) }.padding(8.dp),
                        )
                    }
                    Text(
                        text = "＋",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { showInput = true }.padding(8.dp),
                    )
                }
                HorizontalDivider()
            }
            if (canCopy) {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.kagari_dm_copy)) },
                    modifier = Modifier.clickable(onClick = onCopy),
                )
            }
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.kagari_dm_delete_for_me)) },
                modifier = Modifier.clickable(onClick = onDeleteForMe),
            )
        }
    }
    if (showInput) {
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInput = false },
            title = { Text(text = stringResource(R.string.kagari_dm_reaction_input)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = value.isNotBlank(),
                    onClick = {
                        showInput = false
                        onReact(kagariFirstGrapheme(value.trim()))
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInput = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

private fun kagariFirstGrapheme(text: String): String {
    val iterator = java.text.BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val end = iterator.next()
    return if (end == java.text.BreakIterator.DONE) text else text.substring(0, end)
}

@Composable
internal fun KagariDMRequestBar(
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.kagari_dm_reject))
        }
        Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.kagari_dm_accept))
        }
    }
}
