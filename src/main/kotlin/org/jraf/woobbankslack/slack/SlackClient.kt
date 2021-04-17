/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2021-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.woobbankslack.slack

import okhttp3.OkHttpClient
import org.jraf.woobbankslack.slack.retrofit.SlackRetrofitService
import org.jraf.woobbankslack.slack.retrofit.apimodels.query.SlackApiPostMessageQuery
import org.jraf.woobbankslack.slack.retrofit.apimodels.response.SlackApiChannel
import org.jraf.woobbankslack.slack.retrofit.apimodels.response.SlackApiConversationsListResponse
import org.jraf.woobbankslack.util.Log
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class SlackClient(private val authTokenProvider: AuthTokenProvider) {

    private fun trustAllCertsSslSocketFactory() = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts(), SecureRandom())
    }.socketFactory


    private fun trustAllCerts() = arrayOf<X509TrustManager>(
        object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOf()
            override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
            override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        }
    )

    private fun createRetrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient.Builder()
                //.sslSocketFactory(trustAllCertsSslSocketFactory(), trustAllCerts()[0])
                //.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("localhost", 8888)))
                .build()
        )
        .baseUrl(SLACK_BASE_URI)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val service: SlackRetrofitService = createRetrofit().create(SlackRetrofitService::class.java)

    suspend fun oauthAccess(code: String): String? {
        try {
            val oauthAccessResponse = service.oauthAccess(
                code = code,
                clientId = SLACK_APP_CLIENT_ID,
                clientSecret = SLACK_APP_CLIENT_SECRET
            )
            if (!oauthAccessResponse.ok) return null
            return oauthAccessResponse.authedUser?.accessToken
        } catch (e: Exception) {
            Log.w(e, "Could not make network call")
            return null
        }
    }

    suspend fun postMessage(text: String, channel: String): Boolean {
        val query = SlackApiPostMessageQuery(channel = channel, text = text)
        return try {
            val response = service.postMessage(
                authorization = getAuthorizationHeader(),
                query = query
            )
            response.ok
        } catch (e: Exception) {
            Log.w(e, "Could not make network call")
            false
        }
    }

    suspend fun getChannelList(): List<SlackChannel>? {
        val res = mutableListOf<SlackChannel>()
        var slackApiConversationsListResponse: SlackApiConversationsListResponse? = null
        return try {
            do {
                slackApiConversationsListResponse = service.conversationsList(
                    authorization = getAuthorizationHeader(),
                    cursor = slackApiConversationsListResponse?.responseMetadata?.nextCursor?.ifEmpty { null }
                )
                res += slackApiConversationsListResponse.channels.map { it.toBusiness() }
            } while (!slackApiConversationsListResponse?.responseMetadata?.nextCursor.isNullOrEmpty())
            res.sortedBy { it.name }
        } catch (e: Exception) {
            Log.w(e, "Could not make network call")
            null
        }
    }

    private fun getAuthorizationHeader() = "Bearer ${authTokenProvider.getAuthToken()}"

    companion object {
        private const val SLACK_BASE_URI = "https://slack.com/api/"
        private const val SLACK_APP_CLIENT_ID = "60118040739.208501279878"
        private const val SLACK_APP_CLIENT_SECRET = "251bde8c438c5a0a8ef83d29c5663626"

        const val SLACK_APP_AUTHORIZE_URL =
            "https://slack.com/oauth/v2/authorize?client_id=$SLACK_APP_CLIENT_ID&scope=&user_scope=files:write,channels:read,groups:read"
    }
}

private fun SlackApiChannel.toBusiness() = SlackChannel(
    name = name,
    topic = topic?.value?.ifEmpty { null },
    purpose = purpose?.value?.ifEmpty { null },
)
