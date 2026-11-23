@file:OptIn(ExperimentalSerializationApi::class)

package app.oltnm.data.model

import app.oltnm.data.api.JsonConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Emoji(
    val shortcode: String = "",
    val url: String = "",
    @SerialName("static_url") val staticUrl: String = "",
    val visibleInPicker: Boolean = true
)

@Serializable
data class Account(
    val id: String = "",
    val username: String = "",
    val acct: String = "",
    @SerialName("display_name") val displayName: String = "",
    val note: String = "",
    val url: String = "",
    val avatar: String = "",
    val header: String = "",
    val bot: Boolean = false,
    val locked: Boolean = false,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("statuses_count") val statusesCount: Int = 0,
    val emojis: List<Emoji> = emptyList()
) {
    val nameOrHandle: String
        get() = displayName.ifBlank { username }
}

@Serializable
data class Relationship(
    val id: String = "",
    val following: Boolean = false,
    @SerialName("followed_by") val followedBy: Boolean = false,
    val requested: Boolean = false,
    val blocking: Boolean = false,
    val muting: Boolean = false,
    @SerialName("domain_blocking") val domainBlocking: Boolean = false,
    val endorsed: Boolean = false,
    val notifying: Boolean = false
)

@Serializable
data class MediaAttachment(
    val id: String = "",
    val type: String = "image",
    val url: String = "",
    @SerialName("preview_url") val previewUrl: String = "",
    val description: String? = null,
    val blurhash: String? = null
) {
    val bestUrl: String
        get() = url.ifBlank { previewUrl }

    val bestPreview: String
        get() = previewUrl.ifBlank { url }
}

@Serializable
data class PollOption(
    val title: String = "",
    @SerialName("votes_count") val votesCount: Int? = null
)

@Serializable
data class Poll(
    val id: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    val expired: Boolean = false,
    val multiple: Boolean = false,
    @SerialName("votes_count") val votesCount: Int = 0,
    @SerialName("voters_count") val votersCount: Int? = null,
    val voted: Boolean = false,
    @SerialName("own_votes") val ownVotes: List<Int> = emptyList(),
    val options: List<PollOption> = emptyList()
)

@Serializable
data class Status(
    val id: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val account: Account = Account(),
    @SerialName("media_attachments") val mediaAttachments: List<MediaAttachment> = emptyList(),
    val mentions: List<Mention> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val emojis: List<Emoji> = emptyList(),
    @SerialName("reblogs_count") val reblogsCount: Int = 0,
    @SerialName("favourites_count") val favouritesCount: Int = 0,
    @SerialName("replies_count") val repliesCount: Int = 0,
    val favourited: Boolean = false,
    val reblogged: Boolean = false,
    val bookmarked: Boolean = false,
    val pinned: Boolean = false,
    val sensitive: Boolean = false,
    @SerialName("spoiler_text") val spoilerText: String = "",
    val visibility: String = "public",
    val language: String? = null,
    val url: String? = null,
    val uri: String? = null,
    @SerialName("in_reply_to_id") val inReplyToId: String? = null,
    val poll: Poll? = null,
    val reblog: Status? = null,
    @SerialName("quote_id") val quoteId: String? = null,
    @SerialName("quote") val quote: JsonElement? = null
) {
    val original: Status
        get() = reblog ?: this

    val quotedStatus: Status?
        get() = quoteCache.value

    val displayUrl: String?
        get() = url ?: uri

    @Transient
    private val quoteCache: Lazy<Status?> = lazy {
        val element = quote ?: return@lazy null
        runCatching {
            val obj = element.jsonObject
            if (obj["quoted_status"] != null) {
                val state = obj["state"]?.jsonPrimitive?.contentOrNull
                if (state != null && state != "accepted") {
                    null
                } else {
                    JsonConfig.json.decodeFromJsonElement(
                        Status.serializer(),
                        obj.getValue("quoted_status")
                    )
                }
            } else if (obj["account"] != null) {
                JsonConfig.json.decodeFromJsonElement(Status.serializer(), element)
            } else {
                null
            }
        }.getOrNull()
    }
}

@Serializable
data class StatusContext(
    val ancestors: List<Status> = emptyList(),
    val descendants: List<Status> = emptyList()
)

@Serializable
data class Mention(
    val id: String = "",
    val username: String = "",
    val acct: String = "",
    val url: String = ""
)

@Serializable
data class Tag(
    val name: String = "",
    val url: String = "",
    val following: Boolean = false
)

@Serializable
data class Announcement(
    val id: String = "",
    val content: String = "",
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("all_day") val allDay: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val read: Boolean = false,
    val emojis: List<Emoji> = emptyList()
)

@Serializable
data class MastodonFilter(
    val id: String = "",
    val title: String = "",
    val context: List<String> = emptyList(),
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("filter_action") val filterAction: String = "warn",
    val keywords: List<FilterKeyword> = emptyList()
)

@Serializable
data class FilterKeyword(
    val id: String = "",
    val keyword: String = "",
    @SerialName("whole_word") val wholeWord: Boolean = true
)

@Serializable
data class FilterRequest(
    val title: String,
    val context: List<String>,
    @SerialName("filter_action") val filterAction: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("keywords_attributes") val keywords: List<FilterKeywordRequest>
)

@Serializable
data class FilterKeywordRequest(
    val keyword: String,
    @SerialName("whole_word") val wholeWord: Boolean = true
)

@Serializable
data class Notification(
    val id: String = "",
    val type: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val account: Account = Account(),
    val status: Status? = null
)

@Serializable
data class SearchResults(
    val accounts: List<Account> = emptyList(),
    val statuses: List<Status> = emptyList(),
    val hashtags: List<Tag> = emptyList()
)

@Serializable
data class UploadedMedia(
    val id: String = "",
    val url: String = "",
    @SerialName("preview_url") val previewUrl: String = "",
    val description: String? = null,
    val type: String = "image"
)

@Serializable
data class PollRequest(
    val options: List<String>,
    @SerialName("expires_in") val expiresIn: Int,
    val multiple: Boolean = false
)

@Serializable
data class ScheduledStatus(
    val id: String = "",
    @SerialName("scheduled_at") val scheduledAt: String = "",
    val params: ScheduledStatusParams? = null
)

@Serializable
data class ReportRequest(
    @SerialName("account_id") val accountId: String,
    @SerialName("status_ids") val statusIds: List<String> = emptyList(),
    val comment: String = "",
    val forward: Boolean = false
)

@Serializable
data class StatusRequest(
    val status: String,
    @EncodeDefault val visibility: String = "public",
    @EncodeDefault @SerialName("spoiler_text") val spoilerText: String = "",
    @SerialName("in_reply_to_id") val inReplyToId: String? = null,
    @EncodeDefault val sensitive: Boolean = false,
    @EncodeDefault @SerialName("media_ids") val mediaIds: List<String> = emptyList(),
    val poll: PollRequest? = null,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("quoted_status_id") val quotedStatusId: String? = null,
    @SerialName("quote_approval_policy") val quoteApprovalPolicy: String? = null
)
