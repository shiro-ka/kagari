package dev.dimension.flare.ui.screen.dm

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.common.PagingState
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.BackButton
import dev.dimension.flare.ui.component.dm.dmList
import dev.dimension.flare.ui.presenter.dm.KagariSendViaChatPresenter
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import moe.tlaster.precompose.molecule.producePresenter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariSendViaChatScreen(
    accountKey: MicroBlogKey,
    uri: String,
    cid: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by producePresenter("kagari_send_via_chat_${accountKey}_$uri") {
        remember(accountKey, uri, cid) { KagariSendViaChatPresenter(accountKey, uri, cid) }.invoke()
    }
    var selected by remember { mutableStateOf<MicroBlogKey?>(null) }
    var text by remember { mutableStateOf("") }
    val sentText = stringResource(R.string.kagari_dm_sent)
    val failedText = stringResource(R.string.kagari_dm_action_failed)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.kagari_dm_send_via_chat)) },
                navigationIcon = { BackButton(onBack = onBack) },
            )
        },
        bottomBar = {
            val roomKey = selected
            if (roomKey != null) {
                val rooms = state.rooms as? PagingState.Success
                val roomName =
                    rooms
                        ?.let { list -> (0 until list.itemCount).firstNotNullOfOrNull { index -> list.peek(index)?.takeIf { it.key == roomKey } } }
                        ?.users
                        ?.joinToString { it.name.innerText }
                        .orEmpty()
                Surface(modifier = Modifier.imePadding()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(R.string.kagari_dm_send_to, roomName),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text(text = stringResource(R.string.kagari_dm_send_via_chat_hint)) },
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            enabled = !state.sending,
                            onClick = {
                                state.send(roomKey, text.trim()) { ok ->
                                    if (ok) {
                                        Toast.makeText(context, sentText, Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } else {
                                        Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        ) {
                            Text(text = stringResource(R.string.kagari_dm_send))
                        }
                    }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            dmList(
                data = state.rooms,
                onItemClicked = { selected = it },
            )
        }
    }
}
