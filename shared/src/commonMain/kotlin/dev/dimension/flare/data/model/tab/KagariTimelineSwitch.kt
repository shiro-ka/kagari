package dev.dimension.flare.data.model.tab

import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
import dev.dimension.flare.data.datastore.AppDataStore
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.UiStrings
import dev.dimension.flare.ui.model.UiTimelineV2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public data class KagariTimelineSwitch(
    val options: List<Option>,
    val defaultValue: String,
    val multiple: Boolean = false,
) {
    public data class Option(
        val value: String,
        val title: UiStrings,
    )

    public fun selectedValues(value: String): Set<String> =
        if (multiple) {
            value.split(MULTIPLE_SEPARATOR).filter { it.isNotEmpty() }.toSet()
        } else {
            setOf(value)
        }

    public fun select(
        current: String,
        value: String,
    ): String =
        if (multiple) {
            val selected = selectedValues(current)
            encode(if (value in selected) selected - value else selected + value)
        } else {
            value
        }

    internal fun resolve(value: String?): String =
        when {
            value == null -> defaultValue
            multiple -> encode(selectedValues(value))
            else -> value.takeIf { stored -> options.any { it.value == stored } } ?: defaultValue
        }

    private fun encode(values: Set<String>): String =
        options
            .map { it.value }
            .filter { it in values }
            .joinToString(MULTIPLE_SEPARATOR)

    public companion object {
        public fun toggles(
            options: List<Option>,
            defaultSelected: Set<String>,
        ): KagariTimelineSwitch =
            KagariTimelineSwitch(
                options = options,
                defaultValue = options.map { it.value }.filter { it in defaultSelected }.joinToString(MULTIPLE_SEPARATOR),
                multiple = true,
            )

        private const val MULTIPLE_SEPARATOR = ","
    }
}

@HiddenFromObjC
public fun interface KagariTimelineSwitchProvider<T : TimelineSpec.Data> {
    public fun switch(
        data: T,
        context: TimelineLoaderContext,
    ): Flow<KagariTimelineSwitch?>
}

@HiddenFromObjC
public fun <T : TimelineSpec.Data> KagariTimelineSwitch.asProvider(): KagariTimelineSwitchProvider<T> =
    KagariTimelineSwitchProvider { _, _ -> flowOf(this) }

@HiddenFromObjC
public inline fun <reified S : Any, T> kagariSwitchAccountLoader(
    switch: KagariTimelineSwitch,
    crossinline factory: S.(data: T, value: String) -> RemoteLoader<UiTimelineV2>,
): TimelineLoaderFactory<T>
    where T : TimelineSpec.Data, T : TimelineSpec.AccountData =
    TimelineLoaderFactory { data, context ->
        combine(
            context.accountServiceFlow(AccountType.Specific(data.accountKey)),
            context.kagariSwitchValue(switch),
        ) { service, value ->
            require(service is S) {
                "Expected ${S::class} for ${data.accountKey}, but got ${service::class}"
            }
            service.factory(data, value)
        }
    }

@Single
internal class KagariTimelineSwitchStore(
    private val appDataStore: AppDataStore,
) {
    fun value(
        key: String,
        switch: KagariTimelineSwitch,
    ): Flow<String> =
        appDataStore.kagariTimelineSwitchStore.data
            .map { switch.resolve(it.values[key]) }
            .distinctUntilChanged()

    suspend fun select(
        key: String,
        switch: KagariTimelineSwitch,
        value: String,
    ) {
        appDataStore.kagariTimelineSwitchStore.updateData {
            val current = switch.resolve(it.values[key])
            it.copy(values = it.values + (key to switch.select(current, value)))
        }
    }
}
