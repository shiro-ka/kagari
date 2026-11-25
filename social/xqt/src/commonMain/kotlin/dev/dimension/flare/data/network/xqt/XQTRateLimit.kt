package dev.dimension.flare.data.network.xqt

import de.jensklingenberg.ktorfit.Response

internal val Response<*>.rateLimitRemaining: Int?
    get() = headers["x-rate-limit-remaining"]?.toIntOrNull()
