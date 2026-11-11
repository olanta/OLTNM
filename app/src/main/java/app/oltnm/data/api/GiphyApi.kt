package app.oltnm.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

data class GifResult(
    val id: String,
    val url: String,
    val preview: String,
    val width: Int,
    val height: Int,
    val alt: String
)

data class GifPage(
    val items: List<GifResult>,
    val offset: Int,
    val count: Int,
    val total: Int
)

data class GifDownload(
    val bytes: ByteArray,
    val contentType: String
)

class GiphyApi(private val http: OkHttpClient = HttpClient.shared) {
    suspend fun trending(offset: Int = 0, lang: String = "en"): GifPage = load(TRENDING, query = null, offset = offset, lang = lang)

    suspend fun search(query: String, offset: Int = 0, lang: String = "en"): GifPage = load(SEARCH, query = query, offset = offset, lang = lang)

    suspend fun download(url: String): GifDownload = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "image/*")
            .build()
        http.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes() ?: ByteArray(0)
            if (!response.isSuccessful || bytes.isEmpty()) {
                throw MastodonApiException(response.code, "GIF download failed")
            }
            GifDownload(
                bytes = bytes,
                contentType = response.body?.contentType()?.toString() ?: "image/gif"
            )
        }
    }

    private suspend fun load(
        endpoint: String,
        query: String?,
        offset: Int,
        lang: String
    ): GifPage = withContext(Dispatchers.IO) {
        val builder = endpoint.toHttpUrl().newBuilder()
            .addQueryParameter("api_key", PUBLIC_KEY)
            .addQueryParameter("rating", "g")
            .addQueryParameter("limit", PER_PAGE.toString())
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("lang", lang.take(2).ifBlank { "en" })
        if (query != null) {
            builder.addQueryParameter("q", query)
            builder.addQueryParameter("bundle", "messaging_non_clips")
        }
        val request = Request.Builder().url(builder.build()).build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw MastodonApiException(response.code, "Giphy HTTP ${response.code}")
            }
            val payload = JsonConfig.decode<GiphyResponse>(body)
            val items = payload.data.mapNotNull(::mapItem)
            GifPage(
                items = items,
                offset = payload.pagination.offset,
                count = payload.pagination.count,
                total = payload.pagination.totalCount
            )
        }
    }

    private fun mapItem(item: GiphyItem): GifResult? {
        val images = item.images
        val preview = images.fixedHeightSmall
            ?: images.fixedHeightDownsampled
            ?: images.fixedHeight
            ?: return null
        val original = images.original ?: preview
        val previewUrl = preview.url?.let(::stripQuery) ?: return null
        val originalUrl = original.url?.let(::stripQuery) ?: return null
        return GifResult(
            id = item.id,
            url = originalUrl,
            preview = previewUrl,
            width = preview.width?.toIntOrNull() ?: 0,
            height = preview.height?.toIntOrNull() ?: 0,
            alt = item.altText ?: item.title.takeIf { it.isNotBlank() } ?: "GIF"
        )
    }

    private fun stripQuery(value: String): String = value.toHttpUrlOrNull()?.newBuilder()?.query(null)?.build()?.toString() ?: value

    companion object {
        val PUBLIC_KEY = "Gc7131jiJuvI7IdN0HZ1D7nh0ow5BU6g"
        const val PER_PAGE = 20
        private const val SEARCH = "https://api.giphy.com/v1/gifs/search"
        private const val TRENDING = "https://api.giphy.com/v1/gifs/trending"
    }
}

@Serializable
private data class GiphyResponse(
    val data: List<GiphyItem> = emptyList(),
    val pagination: GiphyPagination = GiphyPagination()
)

@Serializable
private data class GiphyPagination(
    val offset: Int = 0,
    val count: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0
)

@Serializable
private data class GiphyItem(
    val id: String = "",
    val title: String = "",
    @SerialName("alt_text") val altText: String? = null,
    val images: GiphyImages = GiphyImages()
)

@Serializable
private data class GiphyImages(
    @SerialName("fixed_height_small") val fixedHeightSmall: GiphyImage? = null,
    @SerialName("fixed_height_downsampled") val fixedHeightDownsampled: GiphyImage? = null,
    @SerialName("fixed_height") val fixedHeight: GiphyImage? = null,
    val original: GiphyImage? = null
)

@Serializable
private data class GiphyImage(
    val url: String? = null,
    val width: String? = null,
    val height: String? = null
)
