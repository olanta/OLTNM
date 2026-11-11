package app.oltnm.data.api

import app.oltnm.data.model.Account
import app.oltnm.data.model.AppRegistration
import app.oltnm.data.model.Emoji
import app.oltnm.data.model.InstanceDetails
import app.oltnm.data.model.InstanceInfo
import app.oltnm.data.model.InstanceLimits
import app.oltnm.data.model.MastodonList
import app.oltnm.data.model.Notification
import app.oltnm.data.model.ScheduledStatus
import app.oltnm.data.model.SearchResults
import app.oltnm.data.model.Status
import app.oltnm.data.model.StatusRequest
import app.oltnm.data.model.Tag
import app.oltnm.data.model.TokenResponse
import app.oltnm.data.model.TrendLink
import app.oltnm.data.model.UploadedMedia
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MastodonApi(
    private val baseUrl: String,
    private val accessToken: String? = null,
    private val http: OkHttpClient = HttpClient.shared,
    private val json: JsonConfig = JsonConfig
) {
    suspend fun verifyCredentials(): Account = getJson<Account>("/api/v1/accounts/verify_credentials")

    suspend fun account(accountId: String): Account = getJson<Account>("/api/v1/accounts/$accountId")

    suspend fun instanceDetails(): InstanceDetails {
        val v2 = runCatching { getJson<InstanceLimits>("/api/v2/instance") }.getOrNull()
        val v1 = if (v2?.configuration?.statuses?.maxCharacters == null) {
            runCatching { getJson<InstanceLimits>("/api/v1/instance") }.getOrNull()
        } else {
            null
        }
        val max = v2?.configuration?.statuses?.maxCharacters?.takeIf { it > 0 }
            ?: v1?.maxTootChars?.takeIf { it > 0 }
            ?: DEFAULT_MAX_CHARS
        return InstanceDetails(
            maxCharacters = max,
            version = v2?.version ?: v1?.version
        )
    }

    suspend fun maxCharacters(): Int = instanceDetails().maxCharacters

    suspend fun homeTimeline(maxId: String? = null, limit: Int = 20): Page<Status> {
        val raw = getRaw(
            "/api/v1/timelines/home",
            mapOf("max_id" to maxId, "limit" to limit.toString())
        )
        return Page(
            items = json.decodeList<Status>(raw.body),
            nextMaxId = Pagination.nextMaxId(raw.headers["Link"])
        )
    }

    suspend fun publicTimeline(local: Boolean = false, maxId: String? = null, limit: Int = 20): Page<Status> {
        val raw = getRaw(
            "/api/v1/timelines/public",
            mapOf(
                "local" to local.toString(),
                "max_id" to maxId,
                "limit" to limit.toString()
            )
        )
        return Page(
            items = json.decodeList<Status>(raw.body),
            nextMaxId = Pagination.nextMaxId(raw.headers["Link"])
        )
    }

    suspend fun notifications(
        maxId: String? = null,
        limit: Int = 30,
        types: List<String>? = null
    ): Page<Notification> {
        val base = url(
            "/api/v1/notifications",
            mapOf("max_id" to maxId, "limit" to limit.toString())
        )
        val requestUrl = if (types.isNullOrEmpty()) {
            base
        } else {
            base.newBuilder().apply {
                types.forEach { addQueryParameter("types[]", it) }
            }.build()
        }
        val raw = execute(request(requestUrl, "GET"))
        return Page(
            items = json.decodeList<Notification>(raw.body),
            nextMaxId = Pagination.nextMaxId(raw.headers["Link"])
        )
    }

    suspend fun hashtagTimeline(
        tag: String,
        maxId: String? = null,
        limit: Int = 20
    ): Page<Status> {
        val encoded = URLEncoder.encode(tag, "UTF-8").replace("+", "%20")
        val raw = getRaw(
            "/api/v1/timelines/tag/$encoded",
            mapOf("max_id" to maxId, "limit" to limit.toString())
        )
        return Page(
            items = json.decodeList<Status>(raw.body),
            nextMaxId = Pagination.nextMaxId(raw.headers["Link"])
        )
    }

    suspend fun listTimeline(
        listId: String,
        maxId: String? = null,
        limit: Int = 20
    ): Page<Status> {
        val raw = getRaw(
            "/api/v1/timelines/list/$listId",
            mapOf("max_id" to maxId, "limit" to limit.toString())
        )
        return Page(
            items = json.decodeList<Status>(raw.body),
            nextMaxId = Pagination.nextMaxId(raw.headers["Link"])
        )
    }

    suspend fun bookmarks(maxId: String? = null, limit: Int = 20): Page<Status> {
        val raw = getRaw(
            "/api/v1/bookmarks",
            mapOf("max_id" to maxId, "limit" to limit.toString())
        )
        return Page(
            items = json.decodeList<Status>(raw.body),
            nextMaxId = Pagination.nextMaxId(raw.headers["Link"])
        )
    }

    suspend fun favourites(maxId: String? = null, limit: Int = 20): Page<Status> {
        val raw = getRaw(
            "/api/v1/favourites",
            mapOf("max_id" to maxId, "limit" to limit.toString())
        )
        return Page(
            items = json.decodeList<Status>(raw.body),
            nextMaxId = Pagination.nextMaxId(raw.headers["Link"])
        )
    }

    suspend fun lists(): List<MastodonList> = getJson("/api/v1/lists")

    suspend fun trendingLinks(limit: Int = 20): List<TrendLink> = getJson("/api/v1/trends/links", mapOf("limit" to limit.toString()))

    suspend fun trendingStatuses(limit: Int = 20): List<Status> = getJson("/api/v1/trends/statuses", mapOf("limit" to limit.toString()))

    suspend fun trendingTags(limit: Int = 20): List<Tag> = getJson("/api/v1/trends/tags", mapOf("limit" to limit.toString()))

    suspend fun scheduledStatuses(limit: Int = 40): List<ScheduledStatus> = getJson("/api/v1/scheduled_statuses", mapOf("limit" to limit.toString()))

    suspend fun deleteScheduledStatus(id: String) {
        execute(request(url("/api/v1/scheduled_statuses/$id", emptyMap()), "DELETE"))
    }

    suspend fun accountStatuses(accountId: String, maxId: String? = null, limit: Int = 20): Page<Status> {
        val raw = getRaw(
            "/api/v1/accounts/$accountId/statuses",
            mapOf(
                "max_id" to maxId,
                "limit" to limit.toString(),
                "exclude_replies" to "true"
            )
        )
        return Page(
            items = json.decodeList<Status>(raw.body),
            nextMaxId = Pagination.nextMaxId(raw.headers["Link"])
        )
    }

    suspend fun search(query: String, limit: Int = 20): SearchResults = getJson<SearchResults>(
        "/api/v2/search",
        mapOf("q" to query, "resolve" to "true", "limit" to limit.toString())
    )

    suspend fun favourite(statusId: String): Status = postJson<Status>("/api/v1/statuses/$statusId/favourite")

    suspend fun unfavourite(statusId: String): Status = postJson<Status>("/api/v1/statuses/$statusId/unfavourite")

    suspend fun reblog(statusId: String): Status = postJson<Status>("/api/v1/statuses/$statusId/reblog")

    suspend fun unreblog(statusId: String): Status = postJson<Status>("/api/v1/statuses/$statusId/unreblog")

    suspend fun postStatus(request: StatusRequest): Status = postRaw("/api/v1/statuses", json.encodeStatusRequest(request))
        .let { json.decode<Status>(it.body) }

    suspend fun scheduleStatus(request: StatusRequest): ScheduledStatus = postRaw("/api/v1/statuses", json.encodeStatusRequest(request))
        .let { json.decode<ScheduledStatus>(it.body) }

    suspend fun customEmojis(): List<Emoji> = getJson<List<Emoji>>("/api/v1/custom_emojis")

    suspend fun uploadMedia(
        fileName: String,
        bytes: ByteArray,
        mimeType: String,
        description: String? = null
    ): UploadedMedia {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                fileName,
                bytes.toRequestBody(mimeType.toMediaType())
            )
            .apply {
                if (!description.isNullOrBlank()) {
                    addFormDataPart("description", description)
                }
            }
            .build()
        val raw = execute(request(url("/api/v1/media", emptyMap()), "POST", body))
        return json.decode<UploadedMedia>(raw.body)
    }

    suspend fun updateMediaDescription(mediaId: String, description: String): UploadedMedia {
        val form = FormBody.Builder()
            .add("description", description)
            .build()
        val raw = execute(request(url("/api/v1/media/$mediaId", emptyMap()), "PUT", form))
        return json.decode<UploadedMedia>(raw.body)
    }

    private fun url(path: String, query: Map<String, String?>): HttpUrl {
        val builder = (baseUrl.trimEnd('/') + path).toHttpUrl().newBuilder()
        query.forEach { (key, value) ->
            if (value != null) builder.addQueryParameter(key, value)
        }
        return builder.build()
    }

    private fun request(url: HttpUrl, method: String, body: okhttp3.RequestBody? = null): Request = Request.Builder()
        .url(url)
        .apply {
            if (!accessToken.isNullOrBlank()) {
                header("Authorization", "Bearer $accessToken")
            }
            method(method, body)
        }
        .build()

    private suspend fun getRaw(path: String, query: Map<String, String?> = emptyMap()): RawResponse = execute(request(url(path, query), "GET"))

    private suspend fun postRaw(path: String, body: String): RawResponse = execute(request(url(path, emptyMap()), "POST", body.toRequestBody(JSON_MEDIA)))

    private suspend inline fun <reified T> getJson(
        path: String,
        query: Map<String, String?> = emptyMap()
    ): T = execute(request(url(path, query), "GET")).let { json.decode(it.body) }

    private suspend inline fun <reified T> postJson(path: String): T = postRaw(path, "{}").let { json.decode(it.body) }

    private suspend fun execute(request: Request): RawResponse = withContext(Dispatchers.IO) {
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw MastodonApiException(response.code, errorMessage(response.code, text))
            }
            RawResponse(text, response.headers)
        }
    }

    private data class RawResponse(val body: String, val headers: Headers)

    companion object {
        const val DEFAULT_MAX_CHARS = 500
        private val JSON_MEDIA = "application/json".toMediaType()

        suspend fun registerApp(
            host: String,
            redirectUri: String,
            scopes: String,
            website: String = "https://oltnclient.github.io",
            http: OkHttpClient = HttpClient.shared
        ): AppRegistration = withContext(Dispatchers.IO) {
            val payload = """
                {"client_name":"OLTN Mobile","redirect_uris":"$redirectUri",
                "scopes":"$scopes","website":"$website"}
            """.trimIndent()
            val request = Request.Builder()
                .url("https://$host/api/v1/apps")
                .post(payload.toRequestBody(JSON_MEDIA))
                .build()
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw MastodonApiException(response.code, errorMessage(response.code, text))
                }
                JsonConfig.decode<AppRegistration>(text)
            }
        }

        suspend fun fetchInstance(
            host: String,
            http: OkHttpClient = HttpClient.shared
        ): InstanceInfo = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://$host/api/v1/instance")
                .build()
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw MastodonApiException(response.code, errorMessage(response.code, text))
                }
                JsonConfig.decode<InstanceInfo>(text)
            }
        }

        suspend fun fetchToken(
            host: String,
            clientId: String,
            clientSecret: String,
            code: String,
            redirectUri: String,
            http: OkHttpClient = HttpClient.shared
        ): TokenResponse = withContext(Dispatchers.IO) {
            val form = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("redirect_uri", redirectUri)
                .add("scope", "read write follow push")
                .build()
            val request = Request.Builder()
                .url("https://$host/oauth/token")
                .post(form)
                .build()
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw MastodonApiException(response.code, errorMessage(response.code, text))
                }
                JsonConfig.decode<TokenResponse>(text)
            }
        }

        private fun errorMessage(code: Int, body: String): String = runCatching {
            JsonConfig.json.parseToJsonElement(body)
                .jsonObject["error"]
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull() ?: "HTTP $code"
    }
}
