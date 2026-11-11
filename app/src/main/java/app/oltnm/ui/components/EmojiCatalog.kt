package app.oltnm.ui.components

import android.content.Context
import app.oltnm.R
import app.oltnm.data.api.JsonConfig
import kotlinx.serialization.Serializable

@Serializable
data class EmojiGroupData(
    val id: Int = 0,
    val label: String = "",
    val emojis: List<EmojiItemData> = emptyList()
)

@Serializable
data class EmojiItemData(
    val c: String = "",
    val l: String = "",
    val t: List<String> = emptyList(),
    val s: List<String> = emptyList()
)

object EmojiCatalog {
    fun load(context: Context, language: String): List<EmojiGroupData> {
        val resId = if (language.startsWith("pt")) R.raw.emojis_pt else R.raw.emojis_en
        return runCatching {
            val text = context.resources.openRawResource(resId)
                .bufferedReader()
                .use { it.readText() }
            JsonConfig.decode<List<EmojiGroupData>>(text)
        }.getOrDefault(emptyList())
    }
}
