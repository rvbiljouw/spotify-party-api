package uk.bipush.party.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.org.apache.xpath.internal.operations.Bool
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory


object Slack {

    val logger = LoggerFactory.getLogger(Slack::class.java)

    val CLIENT_ID = System.getenv("SLACK_CLIENT_ID")
    val CLIENT_SECRET = System.getenv("SLACK_CLIENT_SECRET")
    val API_HOST = System.getenv("API_HOST") ?: "http://localhost:8080"
    val REDIRECT_URI = "$API_HOST/slack/callback"

    val mapper = ObjectMapper().registerModule(KotlinModule())

    fun authSlackUser(code: String): SlackAuthResponse? {
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("client_id", CLIENT_ID)
                .addFormDataPart("client_secret", CLIENT_SECRET)
                .addFormDataPart("code", code)
                .addFormDataPart("redirect_uri", REDIRECT_URI)
                .build()

        val request = Request.Builder()
                .url("https://slack.com/api/oauth.access")
                .post(requestBody)
                .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val res: SlackAuthResponse = mapper.readValue(response.body()!!.byteStream())
            response.close()

            return res
        } else {
            response.close()
            logger.error("Slack auth failed [$response]")
            return null
        }
    }
}

data class SlackAuthResponse(var ok: Boolean?, @JsonProperty("access_token") var token: String?,
                             var scope: String?, var user: SlackUser?, var team: SlackTeam?)

data class SlackUser(var name: String?, var id: String?)
data class SlackTeam(var id: String?)