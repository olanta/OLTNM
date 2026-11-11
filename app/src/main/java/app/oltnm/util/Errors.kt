package app.oltnm.util

import app.oltnm.data.api.MastodonApiException
import java.io.IOException
import kotlinx.coroutines.CancellationException

fun Throwable.userMessage(): String = when (this) {
    is CancellationException -> ""
    is MastodonApiException -> when (statusCode) {
        401, 403 -> "Session expired. Sign in again."
        404 -> "Not found."
        422 -> message
        429 -> "Rate limited by the server. Try again in a moment."
        else -> message
    }
    is IOException -> "Network error. Check your connection."
    else -> message ?: "Something went wrong."
}
