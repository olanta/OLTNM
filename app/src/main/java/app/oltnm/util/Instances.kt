package app.oltnm.util

import java.net.URLEncoder

object Instances {
    fun normalize(raw: String): String? {
        val host = raw.trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        if (host.isEmpty()) return null
        if (host.contains('/')) return null
        if (host.any { it.isWhitespace() }) return null
        if (!host.contains('.')) return null
        return host
    }

    fun baseUrl(host: String): String = "https://$host"

    fun authorizeUrl(
        host: String,
        clientId: String,
        redirectUri: String,
        scopes: String
    ): String {
        val params = linkedMapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to scopes,
            "force_login" to "true"
        )
        val query = params.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "${baseUrl(host)}/oauth/authorize?$query"
    }
}
