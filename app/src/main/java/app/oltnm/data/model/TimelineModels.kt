package app.oltnm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object TimelineTypes {
    const val HOME = "home"
    const val LOCAL = "local"
    const val FEDERATED = "federated"
    const val REMOTE_LOCAL = "remote-local"
    const val REMOTE_FEDERATED = "remote-federated"
    const val HASHTAG = "hashtag"
    const val LIST = "list"
    const val BOOKMARKS = "bookmarks"
    const val FAVOURITES = "favourites"
    const val MENTIONS = "mentions"
}

@Serializable
data class TimelineSpec(
    val id: String,
    val type: String,
    val host: String? = null,
    val value: String? = null,
    val label: String = ""
)

@Serializable
data class MastodonList(
    val id: String = "",
    val title: String = "",
    val exclusive: Boolean = false
)

@Serializable
data class TrendHistory(
    val day: String = "",
    val uses: Int = 0,
    val accounts: Int = 0
)

@Serializable
data class TrendLink(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val description: String = "",
    @SerialName("provider_name") val providerName: String = "",
    val image: String? = null,
    val history: List<TrendHistory> = emptyList()
) {
    val uses: Int
        get() = history.sumOf { it.uses }
}

@Serializable
data class NewsletterSource(
    val id: String = "",
    val acct: String,
    val name: String = ""
)

@Serializable
data class ScheduledStatusParams(
    val text: String = "",
    @SerialName("spoiler_text") val spoilerText: String = "",
    val visibility: String = "public",
    val sensitive: Boolean = false
)
