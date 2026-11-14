package dev.dimension.flare.ui.presenter.login

private val kagariHiddenPlatforms = setOf("Nostr")

internal fun LoginPlatformProvider.kagariShownInLoginList(): Boolean = platformId !in kagariHiddenPlatforms
