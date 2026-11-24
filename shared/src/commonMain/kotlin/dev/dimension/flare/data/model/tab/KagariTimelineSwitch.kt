package dev.dimension.flare.data.model.tab

import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
import dev.dimension.flare.data.datastore.AppDataStore
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.model.UiStrings
import dev.dimension.flare.ui.model.UiTimelineV2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public data class KagariTimelineSwitch(
    val options: List<Option>,
    val defaultValue: String,
) {
    public data class Option(
        val value: String,
        val title: UiStrings,
    )

    internal fun resolve(value: String?): String = value?.takeIf { stored -> options.any { it.value == stored } } ?: defaultValue
}

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

    suspend fun set(
        key: String,
        value: String,
    ) {
        appDataStore.kagariTimelineSwitchStore.updateData {
            it.copy(values = it.values + (key to value))
        }
    }
}
