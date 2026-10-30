package com.example.data.network

import com.example.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Intercepteur HTTP pour Campus Edu v2 :
 * - Attachement du jeton d'accès si disponible
 * - Envoi de l'en-tête X-App-Version pour le contrôle de version côté serveur (CE-15)
 */
class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("X-App-Version", BuildConfig.VERSION_NAME)
            .header("Accept", "application/json")

        val token = tokenProvider.getAccessToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
