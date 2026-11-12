package app.oltnm.util

object LogRedactor {
    private val querySecret = Regex(
        "(?i)(access_token|refresh_token|client_secret|token|code|authorization)=([^&\\s]+)"
    )
    private val jsonSecret = Regex(
        "(?i)(\"?(?:access_token|refresh_token|client_secret|token|code)\"?\\s*[:=]\\s*\"?)([^\"\\s,&}]+)"
    )
    private val bearer = Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+")
    private val basic = Regex("(?i)(basic\\s+)[A-Za-z0-9+/=]+")

    fun redactUrl(url: String): String = url.substringBefore('?').substringBefore('#')

    fun redactText(text: String): String = text
        .replace(bearer, "$1[redacted]")
        .replace(basic, "$1[redacted]")
        .replace(querySecret, "$1=[redacted]")
        .replace(jsonSecret, "$1[redacted]")
}
