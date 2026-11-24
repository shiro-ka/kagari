package dev.dimension.flare.ui.presenter.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.dimension.flare.data.model.tab.KagariTimelineSwitchStore
import dev.dimension.flare.data.model.tab.TimelineResolver
import dev.dimension.flare.data.model.tab.UiTimelineTabItem
import dev.dimension.flare.di.koinInject
import dev.dimension.flare.ui.model.UiText
import dev.dimension.flare.ui.model.asText
import dev.dimension.flare.ui.presenter.PresenterBase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

public class KagariTimelineSwitchPresenter(
    private val item: UiTimelineTabItem,
) : PresenterBase<KagariTimelineSwitchPresenter.State>() {
    private val scope by koinInject<CoroutineScope>()
    private val timelineResolver: TimelineResolver by koinInject()
    private val switchStore: KagariTimelineSwitchStore by koinInject()

    @Immutable
    public data class Option(
        val value: String,
        val title: UiText,
    )

    public interface State {
        public val options: ImmutableList<Option>
        public val selected: String?

        public fun select(value: String)
    }

    @Composable
    override fun body(): State {
        val target = remember(item) { timelineResolver.kagariSwitchTarget(item) }
        val options =
            remember(target) {
                target
                    ?.second
                    ?.options
                    ?.map { Option(value = it.value, title = it.title.asText()) }
                    ?.toImmutableList()
                    ?: persistentListOf()
            }
        val selectedFlow: Flow<String?> =
            remember(target) {
                if (target == null) {
                    flowOf(null)
                } else {
                    switchStore.value(target.first, target.second)
                }
            }
        val selected by selectedFlow.collectAsState(null)
        return object : State {
            override val options = options
            override val selected = selected

            override fun select(value: String) {
                val key = target?.first ?: return
                scope.launch {
                    switchStore.set(key, value)
                }
            }
        }
    }
}
