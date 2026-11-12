package app.oltnm.data.repo

import app.oltnm.BuildConfig
import app.oltnm.data.api.HttpClient
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.InstanceInfo
import app.oltnm.data.model.PendingLogin
import app.oltnm.data.model.Session
import app.oltnm.data.store.SessionStore
import app.oltnm.util.Instances
import app.oltnm.util.OltnLog
import okhttp3.OkHttpClient

class AuthRepository(
    private val sessionStore: SessionStore,
    private val http: OkHttpClient = HttpClient.shared
) {
    suspend fun discover(rawInstance: String): InstanceInfo {
        val host = Instances.normalize(rawInstance)
            ?: throw IllegalArgumentException("Enter a valid instance, for example mastodon.social")
        OltnLog.d("Discovering instance $host")
        return MastodonApi.fetchInstance(host, http)
    }

    suspend fun beginLogin(rawInstance: String): String {
        val host = Instances.normalize(rawInstance)
            ?: throw IllegalArgumentException("Enter a valid instance, for example mastodon.social")
        val registration = MastodonApi.registerApp(
            host = host,
            redirectUri = BuildConfig.OAUTH_REDIRECT_URI,
            scopes = BuildConfig.OAUTH_SCOPES,
            http = http
        )
        sessionStore.savePending(
            PendingLogin(
                host = host,
                clientId = registration.clientId,
                clientSecret = registration.clientSecret
            )
        )
        OltnLog.i("Starting sign-in for $host")
        return Instances.authorizeUrl(
            host = host,
            clientId = registration.clientId,
            redirectUri = BuildConfig.OAUTH_REDIRECT_URI,
            scopes = BuildConfig.OAUTH_SCOPES
        )
    }

    suspend fun completeLogin(code: String): Session {
        val pending = sessionStore.pending()
            ?: throw IllegalStateException("No sign-in in progress. Start again.")
        val token = MastodonApi.fetchToken(
            host = pending.host,
            clientId = pending.clientId,
            clientSecret = pending.clientSecret,
            code = code,
            redirectUri = BuildConfig.OAUTH_REDIRECT_URI,
            http = http
        )
        val api = MastodonApi(
            baseUrl = Instances.baseUrl(pending.host),
            accessToken = token.accessToken,
            http = http
        )
        val account = api.verifyCredentials()
        val session = Session(
            host = pending.host,
            clientId = pending.clientId,
            clientSecret = pending.clientSecret,
            accessToken = token.accessToken,
            accountId = account.id,
            acct = account.acct,
            displayName = account.nameOrHandle,
            avatar = account.avatar
        )
        sessionStore.saveSession(session)
        sessionStore.clearPending()
        OltnLog.i("Signed in as ${session.handle} on ${session.host}")
        return session
    }
}
