package uk.bipush.party.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.wrapper.spotify.Api
import com.wrapper.spotify.models.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.net.URLEncoder


object Spotify {
    val CLIENT_ID = System.getenv("SPOTIFY_CLIENT_ID")
    val CLIENT_SECRET = System.getenv("SPOTIFY_CLIENT_SECRET")
    val mapper = ObjectMapper().registerModule(KotlinModule())

    fun play(track: String, token: String): String {
        val requestR = mapOf("uris" to arrayOf(track))
        val reqBody = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(requestR))
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/me/player/play")
                .addHeader("Authorization", "Bearer $token")
                .put(reqBody)
                .build()
        val client = OkHttpClient()
        println(track)
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            println(response.message())
            response.close()
            return response.message()
        } else {
            response.close()
            return "error"
        }
    }

    fun getInfo(token: String) {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/me/player")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        println(response)
        println(response.body()?.string())
    }

    fun getDevices(token: String): SpotifyDeviceResponse? {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/me/player/devices")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        if (response.body() != null) {
            val devices: SpotifyDeviceResponse = mapper.readValue(response.body()!!.string())
            response.close()
            return devices
        } else {
            return null
        }
    }

    fun searchSongs(accessToken: String, refreshToken: String, filters: List<SpotifyFilter>, offset: Int = 0, limit: Int = 25): Page<Track>? {
        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .redirectURI("http://localhost:8080/callback")
                .build()

        return api.searchTracks(filters.joinToString(" ") { f -> f.compile() })
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }

    fun searchAlbums(accessToken: String, refreshToken: String, filters: List<SpotifyFilter>, offset: Int = 0, limit: Int = 25): Page<SimpleAlbum>? {
        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .redirectURI("http://localhost:8080/callback")
                .build()

        return api.searchAlbums(filters.joinToString(" ") { f -> f.compile() })
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }

    fun searchArtists(accessToken: String, refreshToken: String, filters: List<SpotifyFilter>, offset: Int = 0, limit: Int = 25): Page<Artist>? {
        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .redirectURI("http://localhost:8080/callback")
                .build()

        return api.searchArtists(filters.joinToString(" ") { f -> f.compile() })
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }
}

data class SpotifyDevice(val id: String,
                         @field:JsonProperty("is_active") val isActive: Boolean,
                         @field:JsonProperty("is_restricted") val isRestricted: Boolean,
                         val name: String,
                         val type: String,
                         @field:JsonProperty("volume_percent") val volumePercent: Double)

data class SpotifyDeviceResponse(val devices: List<SpotifyDevice>)