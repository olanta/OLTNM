package app.oltnm

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.oltnm.data.model.LanguageChoice
import app.oltnm.data.store.PreferencesStore
import app.oltnm.ui.OltnApp
import app.oltnm.util.OAuthBus
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = PreferencesStore.readLanguage(newBase)
        val locale = language.tag?.let { Locale.forLanguageTag(it) }
            ?: newBase.resources.configuration.locales[0]
        Locale.setDefault(locale)
        if (language == LanguageChoice.System) {
            super.attachBaseContext(newBase)
        } else {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        val container = (application as OltnApplication).container
        setContent {
            OltnApp(container)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "oltnm" || data.host != "oauth") return
        val error = data.getQueryParameter("error")
        if (error != null) {
            OAuthBus.deliverError(data.getQueryParameter("error_description") ?: error)
            return
        }
        data.getQueryParameter("code")?.let(OAuthBus::deliverCode)
    }
}
