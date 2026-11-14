@file:OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)

package dev.dimension.flare.ui.screen.home

import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import dev.dimension.flare.ui.component.NavigationSuiteScope2

internal fun NavigationSuiteType.kagariLayoutType(loggedOut: Boolean): NavigationSuiteType =
    if (loggedOut) NavigationSuiteType.NavigationBar else this

internal fun kagariNavItems(
    loggedOut: Boolean,
    items: NavigationSuiteScope2.() -> Unit,
): NavigationSuiteScope2.() -> Unit = if (loggedOut) ({}) else items
