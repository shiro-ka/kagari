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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.EyeSlash
import compose.icons.fontawesomeicons.solid.File
import compose.icons.fontawesomeicons.solid.FileLines
import compose.icons.fontawesomeicons.solid.FilePdf
import compose.icons.fontawesomeicons.solid.FileZipper
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_file_sensitive
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.FlareDividerDefaults
import dev.dimension.flare.ui.component.LocalTimelineAppearance
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.model.KagariFile
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.toUri
import dev.dimension.flare.ui.theme.PlatformTheme
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

@Composable
internal fun KagariFilesComponent(
    files: ImmutableList<KagariFile>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        files.forEach { file ->
            KagariFileRow(file)
        }
    }
}

@Composable
private fun KagariFileRow(file: KagariFile) {
    val appearanceSettings = LocalTimelineAppearance.current
    val uriHandler = LocalUriHandler.current
    var hidden by remember(file.url, appearanceSettings.showSensitiveContent) {
        mutableStateOf(file.sensitive && !appearanceSettings.showSensitiveContent)
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    FlareDividerDefaults.thickness,
                    color = FlareDividerDefaults.color,
                    shape = PlatformTheme.shapes.medium,
                ).clip(PlatformTheme.shapes.medium)
                .clickable {
                    if (hidden) {
                        hidden = false
                    } else {
                        uriHandler.openUri(DeeplinkRoute.OpenLinkDirectly(file.url).toUri())
                    }
                }.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hidden) {
            FAIcon(
                imageVector = FontAwesomeIcons.Solid.EyeSlash,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PlatformTheme.colorScheme.caption,
            )
            PlatformText(
                text = stringResource(Res.string.kagari_file_sensitive),
                color = PlatformTheme.colorScheme.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            FAIcon(
                imageVector = kagariFileIcon(file.type, file.name),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            PlatformText(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            PlatformText(
                text = kagariFileSize(file.size),
                style = PlatformTheme.typography.caption,
                color = PlatformTheme.colorScheme.caption,
                maxLines = 1,
            )
        }
    }
}

private val kagariArchiveTypes =
    setOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/x-7z-compressed",
        "application/x-rar-compressed",
        "application/vnd.rar",
        "application/gzip",
        "application/x-gzip",
        "application/x-tar",
        "application/x-bzip2",
        "application/x-xz",
    )

private val kagariArchiveExtensions = setOf("zip", "7z", "rar", "gz", "tgz", "tar", "bz2", "xz")

private fun kagariFileIcon(
    type: String,
    name: String,
): ImageVector {
    val mime = type.substringBefore(';').trim().lowercase()
    val extension = name.substringAfterLast('.', "").lowercase()
    return when {
        mime == "application/pdf" || extension == "pdf" -> FontAwesomeIcons.Solid.FilePdf
        mime in kagariArchiveTypes || extension in kagariArchiveExtensions -> FontAwesomeIcons.Solid.FileZipper
        mime.startsWith("text/") -> FontAwesomeIcons.Solid.FileLines
        else -> FontAwesomeIcons.Solid.File
    }
}

private fun kagariFileSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.coerceAtLeast(0).toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) {
        "${bytes.coerceAtLeast(0)} ${units[unit]}"
    } else {
        "${(value * 10).roundToLong() / 10.0} ${units[unit]}"
    }
}
