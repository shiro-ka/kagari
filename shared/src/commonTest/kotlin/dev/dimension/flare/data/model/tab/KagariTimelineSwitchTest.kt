package dev.dimension.flare.data.model.tab

import dev.dimension.flare.ui.model.UiStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class KagariTimelineSwitchTest {
    private val toggles =
        KagariTimelineSwitch.toggles(
            options =
                listOf(
                    KagariTimelineSwitch.Option("a", UiStrings.KagariMisskeyWithRenotes),
                    KagariTimelineSwitch.Option("b", UiStrings.KagariMisskeyWithReplies),
                    KagariTimelineSwitch.Option("c", UiStrings.KagariMisskeyWithFiles),
                ),
            defaultSelected = setOf("a"),
        )

    private val single =
        KagariTimelineSwitch(
            options =
                listOf(
                    KagariTimelineSwitch.Option("x", UiStrings.KagariTimelineSwitchPublic),
                    KagariTimelineSwitch.Option("y", UiStrings.KagariTimelineSwitchPrivate),
                ),
            defaultValue = "x",
        )

    @Test
    fun togglesTurnEachOptionOnAndOff() {
        val withC = toggles.select(toggles.defaultValue, "c")
        assertEquals(setOf("a", "c"), toggles.selectedValues(withC))
        val withoutA = toggles.select(withC, "a")
        assertEquals(setOf("c"), toggles.selectedValues(withoutA))
        val none = toggles.select(withoutA, "c")
        assertEquals(emptySet(), toggles.selectedValues(none))
    }

    @Test
    fun togglesResolveDropsUnknownValuesAndKeepsEmptySelection() {
        assertEquals("a", toggles.resolve(null))
        assertEquals("", toggles.resolve(""))
        assertEquals("a,c", toggles.resolve("c,z,a"))
    }

    @Test
    fun singleSelectReplacesValue() {
        assertEquals("y", single.select("x", "y"))
        assertEquals(setOf("y"), single.selectedValues("y"))
        assertEquals("x", single.resolve("z"))
    }
}
