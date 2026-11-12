package app.oltnm.data.api

import app.oltnm.util.LogRedactor
import app.oltnm.util.OltnLog
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

class NetworkLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.method
        val url = LogRedactor.redactUrl(request.url.toString())
        val start = System.nanoTime()
        return try {
            val response = chain.proceed(request)
            OltnLog.d("HTTP $method $url -> ${response.code} (${elapsedMillis(start)}ms)")
            response
        } catch (error: IOException) {
            OltnLog.w("HTTP $method $url failed after ${elapsedMillis(start)}ms: ${error.javaClass.simpleName}")
            throw error
        }
    }

    private fun elapsedMillis(start: Long): Long = (System.nanoTime() - start) / 1_000_000
}
