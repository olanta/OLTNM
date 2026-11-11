package app.oltnm.di

import android.content.Context
import app.oltnm.data.api.GiphyApi
import app.oltnm.data.api.HttpClient
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.api.TranslateApi
import app.oltnm.data.model.Session
import app.oltnm.data.repo.AuthRepository
import app.oltnm.data.store.DraftStore
import app.oltnm.data.store.NewsletterStore
import app.oltnm.data.store.PreferencesStore
import app.oltnm.data.store.SessionStore
import app.oltnm.data.store.TimelineStore
import app.oltnm.util.Instances

class AppContainer(context: Context) {
    private val http = HttpClient.shared

    val sessionStore = SessionStore(context)
    val preferences = PreferencesStore(context)
    val draftStore = DraftStore(context)
    val timelineStore = TimelineStore(context)
    val newsletterStore = NewsletterStore(context)
    val authRepository = AuthRepository(sessionStore, http)
    val translateApi = TranslateApi(http)
    val giphyApi = GiphyApi(http)

    fun apiFor(session: Session): MastodonApi = MastodonApi(
        baseUrl = Instances.baseUrl(session.host),
        accessToken = session.accessToken,
        http = http
    )

    fun publicApiForHost(host: String): MastodonApi = MastodonApi(
        baseUrl = Instances.baseUrl(host),
        accessToken = null,
        http = http
    )
}
