package app.oltnm

import android.app.Application
import app.oltnm.di.AppContainer
import app.oltnm.util.CrashReporter
import app.oltnm.util.OltnLog

class OltnApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        container = AppContainer(this)
        OltnLog.i("App started (${BuildConfig.VERSION_NAME} ${BuildConfig.VERSION_CODE})")
    }
}
