package dev.dimension.flare.data.datasource.misskey

import dev.dimension.flare.data.model.tab.KagariTimelineSwitch
import dev.dimension.flare.ui.model.UiStrings

internal enum class MisskeyTimelineFilter(
    val value: String,
    val title: UiStrings,
) {
    Renotes("renotes", UiStrings.KagariMisskeyWithRenotes),
    Replies("replies", UiStrings.KagariMisskeyWithReplies),
    Files("files", UiStrings.KagariMisskeyWithFiles),
}

internal data class MisskeyTimelineFilters(
    val withRenotes: Boolean = true,
    val withReplies: Boolean = false,
    val withFiles: Boolean = false,
) {
    val key: String
        get() =
            buildString {
                if (withRenotes) append('r')
                if (withReplies) append('p')
                if (withFiles) append('f')
            }
}

internal fun misskeyTimelineSwitch(vararg filters: MisskeyTimelineFilter): KagariTimelineSwitch =
    KagariTimelineSwitch.toggles(
        options = filters.map { KagariTimelineSwitch.Option(it.value, it.title) },
        defaultSelected = setOf(MisskeyTimelineFilter.Renotes.value),
    )

internal fun KagariTimelineSwitch.misskeyTimelineFilters(value: String): MisskeyTimelineFilters {
    val selected = selectedValues(value)
    return MisskeyTimelineFilters(
        withRenotes = MisskeyTimelineFilter.Renotes.value in selected,
        withReplies = MisskeyTimelineFilter.Replies.value in selected,
        withFiles = MisskeyTimelineFilter.Files.value in selected,
    )
}

internal object MisskeyTimelineSwitches {
    val home: KagariTimelineSwitch = misskeyTimelineSwitch(MisskeyTimelineFilter.Renotes, MisskeyTimelineFilter.Files)
    val local: KagariTimelineSwitch =
        misskeyTimelineSwitch(MisskeyTimelineFilter.Renotes, MisskeyTimelineFilter.Replies, MisskeyTimelineFilter.Files)
    val hybrid: KagariTimelineSwitch =
        misskeyTimelineSwitch(MisskeyTimelineFilter.Renotes, MisskeyTimelineFilter.Replies, MisskeyTimelineFilter.Files)
    val global: KagariTimelineSwitch = misskeyTimelineSwitch(MisskeyTimelineFilter.Renotes, MisskeyTimelineFilter.Files)
}
