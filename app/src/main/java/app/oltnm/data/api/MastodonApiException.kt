package app.oltnm.data.api

import java.io.IOException

class MastodonApiException(
    val statusCode: Int,
    override val message: String
) : IOException(message)
