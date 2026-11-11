package app.oltnm.data.api

import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

data class Translation(
    val text: String,
    val source: String?
)

class TranslateApi(private val http: OkHttpClient = HttpClient.shared) {
    suspend fun translate(text: String, target: String): Translation = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("client", "gtx")
            .add("sl", "auto")
            .add("tl", target)
            .add("dt", "t")
            .add("q", text)
            .build()
        val request = Request.Builder().url(ENDPOINT).post(form).build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw MastodonApiException(response.code, "Translate HTTP ${response.code}")
            }
            val root = HttpClient.json.parseToJsonElement(body).jsonArray
            val segments = root.getOrNull(0) as? JsonArray
                ?: throw IllegalStateException("Invalid translation response")
            val translated = buildString {
                segments.forEach { segment ->
                    val part = segment as? JsonArray
                    val value = part?.getOrNull(0) as? JsonPrimitive
                    append(value?.contentOrNull.orEmpty())
                }
            }.trim()
            if (translated.isEmpty()) throw IllegalStateException("Empty translation")
            val source = (root.getOrNull(2) as? JsonPrimitive)?.contentOrNull
            Translation(translated, source)
        }
    }

    companion object {
        private const val ENDPOINT = "https://translate.googleapis.com/translate_a/single"
        val TARGETS = listOf("pt-BR", "en", "es", "fr", "de", "it", "ja")
    }
}

fun languageDisplayName(code: String): String {
    val name = Locale.forLanguageTag(code).getDisplayName(Locale.getDefault())
    return name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
