package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Users
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_community_note_title
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.FlareDividerDefaults
import dev.dimension.flare.ui.component.RichText
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.model.KagariCommunityNote
import dev.dimension.flare.ui.theme.PlatformTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KagariCommunityNoteComponent(
    note: KagariCommunityNote,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val url = note.url
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    FlareDividerDefaults.thickness,
                    color = FlareDividerDefaults.color,
                    shape = PlatformTheme.shapes.medium,
                ).clip(PlatformTheme.shapes.medium)
                .let {
                    if (url != null) {
                        it.clickable { uriHandler.openUri(url) }
                    } else {
                        it
                    }
                }.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FAIcon(
                imageVector = FontAwesomeIcons.Solid.Users,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            PlatformText(
                text = note.title ?: stringResource(Res.string.kagari_community_note_title),
                fontWeight = FontWeight.Bold,
            )
        }
        RichText(
            text = note.text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
