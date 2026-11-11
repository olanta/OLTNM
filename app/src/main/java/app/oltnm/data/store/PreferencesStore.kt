package app.oltnm.data.store

import android.content.Context
import app.oltnm.data.model.Accent
import app.oltnm.data.model.FontFamilyChoice
import app.oltnm.data.model.LanguageChoice
import app.oltnm.data.model.TextColorChoice
import app.oltnm.data.model.TextSizeChoice
import app.oltnm.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accent = MutableStateFlow(readAccent())
    val accent: StateFlow<Accent> = _accent.asStateFlow()

    private val _language = MutableStateFlow(readLanguageSetting())
    val language: StateFlow<LanguageChoice> = _language.asStateFlow()

    private val _fontFamily = MutableStateFlow(readFontFamily())
    val fontFamily: StateFlow<FontFamilyChoice> = _fontFamily.asStateFlow()

    private val _textSize = MutableStateFlow(readTextSize())
    val textSize: StateFlow<TextSizeChoice> = _textSize.asStateFlow()

    private val _textColor = MutableStateFlow(readTextColor())
    val textColor: StateFlow<TextColorChoice> = _textColor.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAccent(accent: Accent) {
        prefs.edit().putString(KEY_ACCENT, accent.name).apply()
        _accent.value = accent
    }

    fun setLanguage(language: LanguageChoice) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _language.value = language
    }

    fun setFontFamily(font: FontFamilyChoice) {
        prefs.edit().putString(KEY_FONT, font.name).apply()
        _fontFamily.value = font
    }

    fun setTextSize(size: TextSizeChoice) {
        prefs.edit().putString(KEY_TEXT_SIZE, size.name).apply()
        _textSize.value = size
    }

    fun setTextColor(color: TextColorChoice) {
        prefs.edit().putString(KEY_TEXT_COLOR, color.name).apply()
        _textColor.value = color
    }

    private fun readThemeMode(): ThemeMode = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null).orEmpty()) }
        .getOrDefault(ThemeMode.Dark)
    private fun readAccent(): Accent = runCatching { Accent.valueOf(prefs.getString(KEY_ACCENT, null).orEmpty()) }
        .getOrDefault(Accent.Green)
    private fun readLanguageSetting(): LanguageChoice = runCatching { LanguageChoice.valueOf(prefs.getString(KEY_LANGUAGE, null).orEmpty()) }
        .getOrDefault(LanguageChoice.System)

    private fun readFontFamily(): FontFamilyChoice = runCatching { FontFamilyChoice.valueOf(prefs.getString(KEY_FONT, null).orEmpty()) }
        .getOrDefault(FontFamilyChoice.SpaceGrotesk)

    private fun readTextSize(): TextSizeChoice = runCatching { TextSizeChoice.valueOf(prefs.getString(KEY_TEXT_SIZE, null).orEmpty()) }
        .getOrDefault(TextSizeChoice.Default)

    private fun readTextColor(): TextColorChoice = runCatching { TextColorChoice.valueOf(prefs.getString(KEY_TEXT_COLOR, null).orEmpty()) }
        .getOrDefault(TextColorChoice.Default)

    companion object {
        private const val PREFS = "oltnm.preferences"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_ACCENT = "accent"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_FONT = "font_family"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_TEXT_COLOR = "text_color"

        /** Read before the Activity base context is attached. */
        fun readLanguage(context: Context): LanguageChoice {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return runCatching {
                LanguageChoice.valueOf(prefs.getString(KEY_LANGUAGE, null).orEmpty())
            }.getOrDefault(LanguageChoice.System)
        }
    }
}
