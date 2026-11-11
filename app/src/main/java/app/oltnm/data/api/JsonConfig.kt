package app.oltnm.data.api

import app.oltnm.data.model.StatusRequest
import kotlinx.serialization.decodeFromString

object JsonConfig {
    val json = HttpClient.json

    inline fun <reified T> decode(body: String): T = json.decodeFromString<T>(body)

    inline fun <reified T> decodeList(body: String): List<T> = json.decodeFromString<List<T>>(body)

    fun encodeStatusRequest(request: StatusRequest): String = json.encodeToString(StatusRequest.serializer(), request)
}
