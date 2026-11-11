package app.oltnm.data.model

enum class Visibility(val apiValue: String) {
    Public("public"),
    Local("local"),
    Unlisted("unlisted"),
    Private("private"),
    Direct("direct")
    ;

    companion object {
        fun fromApi(value: String?): Visibility = Visibility.entries.firstOrNull { it.apiValue == value } ?: Public
    }
}
