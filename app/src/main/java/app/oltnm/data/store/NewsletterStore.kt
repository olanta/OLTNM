package app.oltnm.data.store

import android.content.Context
import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.NewsletterSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer

class NewsletterStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _sources = MutableStateFlow(loadSources())
    val sources: StateFlow<List<NewsletterSource>> = _sources.asStateFlow()

    fun replace(sources: List<NewsletterSource>) {
        prefs.edit()
            .putString(KEY_SOURCES, JsonConfig.json.encodeToString(SOURCE_LIST, sources))
            .apply()
        _sources.value = sources
    }

    fun add(source: NewsletterSource) {
        val current = _sources.value
        if (current.any { it.acct.equals(source.acct, ignoreCase = true) }) return
        replace(current + source)
    }

    fun remove(acct: String) {
        replace(_sources.value.filterNot { it.acct.equals(acct, ignoreCase = true) })
    }

    private fun loadSources(): List<NewsletterSource> {
        val encoded = prefs.getString(KEY_SOURCES, null)
        if (encoded.isNullOrBlank()) return DEFAULTS
        return runCatching {
            JsonConfig.json.decodeFromString(SOURCE_LIST, encoded)
        }.getOrDefault(DEFAULTS)
    }

    private companion object {
        const val PREFS = "oltnm.newsletter"
        const val KEY_SOURCES = "sources"
        val SOURCE_LIST = ListSerializer(NewsletterSource.serializer())

        val DEFAULTS = listOf(
            NewsletterSource(acct = "icnoticias.com.br@rss-parrot.net", name = "icnoticias.com.br"),
            NewsletterSource(acct = "jornalpublicobot@mastodon.world", name = "jornalpublicobot"),
            NewsletterSource(acct = "FlipboardBR@flipboard.com", name = "FlipboardBR"),
            NewsletterSource(acct = "teiadospovos@kolektiva.social", name = "teiadospovos"),
            NewsletterSource(acct = "noticiasbrasil@mastodon.social", name = "noticiasbrasil")
        )
    }
}
