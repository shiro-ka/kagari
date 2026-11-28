package dev.dimension.flare.data.datasource.microblog.paging
import kotlin.native.HiddenFromObjC
import kotlin.time.Duration

@HiddenFromObjC
public interface CacheableRemoteLoader<T : Any> : RemoteLoader<T> {
    public val pagingKey: String
    public val remotePageSize: Int?
        get() = null
    public val canJoinTimeline: Boolean
        get() = false
    public val replacesOnRefresh: Boolean
        get() = false
    public val refreshGapOncePerSession: Boolean
        get() = false
    public val supportPrepend: Boolean
        get() = false
    public val collapseReplyChains: Boolean
        get() = true
    public val gapFillInterval: Duration
        get() = Duration.ZERO
    public val canContinueGapFill: Boolean
        get() = true
}

@HiddenFromObjC
public interface ReportableRemoteLoader {
    public var reportError: ((Throwable) -> Unit)?
}
