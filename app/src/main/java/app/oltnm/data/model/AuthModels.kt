package app.oltnm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppRegistration(
    val id: String = "",
    val name: String = "",
    val website: String? = null,
    @SerialName("redirect_uri") val redirectUri: String = "",
    @SerialName("client_id") val clientId: String = "",
    @SerialName("client_secret") val clientSecret: String = ""
)

@Serializable
data class InstanceLimits(
    val configuration: InstanceLimitsConfiguration? = null,
    @SerialName("max_toot_chars") val maxTootChars: Int? = null,
    val version: String? = null
)

data class InstanceDetails(
    val maxCharacters: Int,
    val version: String?
)

@Serializable
data class InstanceLimitsConfiguration(
    val statuses: InstanceStatusesLimits? = null
)

@Serializable
data class InstanceStatusesLimits(
    @SerialName("max_characters") val maxCharacters: Int? = null
)

@Serializable
data class InstanceInfo(
    val uri: String = "",
    val title: String = "",
    val thumbnail: String? = null,
    val version: String? = null
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "Bearer",
    val scope: String = "",
    @SerialName("created_at") val createdAt: Long = 0
)

@Serializable
data class PendingLogin(
    val host: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String
)

@Serializable
data class Session(
    val host: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("account_id") val accountId: String,
    val acct: String,
    @SerialName("display_name") val displayName: String,
    val avatar: String
)
