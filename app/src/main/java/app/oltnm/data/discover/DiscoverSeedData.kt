package app.oltnm.data.discover

import android.content.Context
import app.oltnm.R
import app.oltnm.data.api.JsonConfig

object DiscoverSeedData {
    fun load(context: Context): Map<String, List<String>> = runCatching {
        val text = context.resources.openRawResource(R.raw.discover_seeds)
            .bufferedReader()
            .use { it.readText() }
        JsonConfig.decode<Map<String, List<String>>>(text)
    }.getOrDefault(emptyMap())
}
