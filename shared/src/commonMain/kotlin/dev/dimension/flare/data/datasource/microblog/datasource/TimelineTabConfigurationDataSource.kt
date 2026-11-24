package dev.dimension.flare.data.datasource.microblog.datasource

import dev.dimension.flare.data.model.tab.ShortcutSpec
import dev.dimension.flare.data.model.tab.TimelineCandidate
import dev.dimension.flare.ui.model.UiStrings
import dev.dimension.flare.ui.model.UiText
import dev.dimension.flare.ui.model.asText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public interface TimelineTabConfigurationDataSource {
    public val defaultTabs: ImmutableList<TimelineCandidate<*>>
    public val builtInTimelineTabs: ImmutableList<TimelineCandidate<*>>
    public val addableTimelineTabs: ImmutableList<TimelineCandidate<*>>
        get() = builtInTimelineTabs
    public val kagariAddableTimelineTabSections: ImmutableList<KagariAddableTimelineTabSection>
        get() =
            persistentListOf(
                KagariAddableTimelineTabSection(
                    title = UiStrings.Default,
                    tabs = addableTimelineTabs,
                ),
            )
    public val shortcuts: ImmutableList<ShortcutSpec>
}

@HiddenFromObjC
public data class KagariAddableTimelineTabSection(
    val title: UiText,
    val tabs: ImmutableList<TimelineCandidate<*>>,
) {
    public constructor(
        title: UiStrings,
        tabs: ImmutableList<TimelineCandidate<*>>,
    ) : this(title = title.asText(), tabs = tabs)
}
