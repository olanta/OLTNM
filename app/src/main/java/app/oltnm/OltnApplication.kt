package app.oltnm

import android.app.Application
import app.oltnm.di.AppContainer

class OltnApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
