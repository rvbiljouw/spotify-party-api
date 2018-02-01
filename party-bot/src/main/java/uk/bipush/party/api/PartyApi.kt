package uk.bipush.party.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.http.util.EntityUtils
import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyAccess
import uk.bipush.party.model.PartyType
import java.util.concurrent.TimeUnit

object PartyApi {

    val API_HOST = System.getenv("API_HOST")

    val mapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JodaModule())

    val client = OkHttpClient().newBuilder()
            .readTimeout(30000L, TimeUnit.MILLISECONDS)
            .build()

    fun createParty(token: String, req: CreatePartyRequest): Party {
        val request = Request.Builder()
                .addHeader("Authorization", "Bearer $token")
                .url("$API_HOST/api/v1/party")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        mapper.writeValueAsString(req)
                )).build()

        client.newCall(request).execute().use { response ->
            return mapper.readValue(String(response.body()!!.bytes()))
        }
    }

    fun queueSong(token: String, party: Party, req: QueueSongRequest): Boolean {
        val request = Request.Builder()
                .addHeader("Authorization", "Bearer $token")
                .url("$API_HOST/api/v1/party/${party.id}/queue")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        mapper.writeValueAsString(req)
                )).build()

        client.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }
}

data class QueueSongRequest(val songId: String, val artist: String, val title: String, val thumbnail: String, val uri: String, val duration: Int, val uploadedBy: String?)

data class CreatePartyRequest(val type: PartyType, val access: PartyAccess, val name: String, val description: String)