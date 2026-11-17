package dev.dimension.flare.data.model.tab

import dev.dimension.flare.di.koinGet

public fun UiTimelineTabItem.kagariHasMultipleAccounts(): Boolean =
    runCatching { koinGet<TimelineResolver>().accountKeys(this).size > 1 }.getOrDefault(false)
