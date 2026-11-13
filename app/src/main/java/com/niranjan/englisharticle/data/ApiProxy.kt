package com.niranjan.englisharticle.data

import android.content.Context
import com.niranjan.englisharticle.BuildConfig
import java.net.HttpURLConnection
import java.util.UUID

/**
 * Where the app sends its AI and text-to-speech requests: the app's own server (see
 * `server/` in the repository), which holds the OpenRouter and Deepgram keys.
 *
 * The app used to call OpenRouter and Deepgram directly with keys compiled into the APK,
 * so anyone who downloaded it could extract them and spend on the owner's accounts. The
 * app now ships only [appToken], which unlocks nothing but the server's three narrow,
 * rate-limited routes, and can be rotated without touching the paid keys.
 */
data class ApiProxy(
    val baseUrl: String,
    val appToken: String,
    /** Random id for this install; the server rate-limits per install. */
    val installId: String
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank()

    fun url(path: String): String = baseUrl.trimEnd('/') + path

    fun applyHeaders(connection: HttpURLConnection) {
        if (appToken.isNotBlank()) connection.setRequestProperty("X-App-Token", appToken)
        connection.setRequestProperty("X-Install-Id", installId)
    }

    companion object {
        private const val PREFS_NAME = "api_proxy"
        private const val INSTALL_ID_KEY = "install_id"

        fun fromBuildConfig(context: Context): ApiProxy = ApiProxy(
            baseUrl = BuildConfig.API_BASE_URL,
            appToken = BuildConfig.API_APP_TOKEN,
            installId = installId(context)
        )

        private fun installId(context: Context): String {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(INSTALL_ID_KEY, null)?.let { return it }
            val created = UUID.randomUUID().toString()
            prefs.edit().putString(INSTALL_ID_KEY, created).apply()
            return created
        }
    }
}
