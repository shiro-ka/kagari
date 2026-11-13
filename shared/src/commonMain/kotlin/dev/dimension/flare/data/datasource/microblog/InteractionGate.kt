package dev.dimension.flare.data.datasource.microblog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class InteractionGate(
    public val reply: Reply = Reply.Anyone,
    public val allowQuote: Boolean = true,
) {
    @Serializable
    public sealed interface Reply {
        @Serializable
        @SerialName("anyone")
        public data object Anyone : Reply

        @Serializable
        @SerialName("nobody")
        public data object Nobody : Reply

        @Serializable
        @SerialName("some")
        public data class Some(
            public val followers: Boolean = false,
            public val following: Boolean = false,
            public val mentioned: Boolean = false,
            public val lists: List<String> = emptyList(),
        ) : Reply {
            public val isEmpty: Boolean
                get() = !followers && !following && !mentioned && lists.isEmpty()
        }
    }

    public companion object {
        public const val MAX_RULES: Int = 5
    }
}

public data class InteractionGateList(
    public val uri: String,
    public val name: String,
)

public class InteractionGateConfig(
    public val loadDefault: suspend () -> InteractionGate,
    public val saveDefault: suspend (InteractionGate) -> Unit,
    public val loadLists: suspend () -> List<InteractionGateList>,
)
