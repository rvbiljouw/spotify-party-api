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
    val API_HOST = System.getenv("API_HOST") ?: "http://localhost:8080"
    val FRONTEND_HOST = System.getenv("FRONTEND_HOST") ?: "http://localhost:4200"

    fun play(track: String, playTargets: List<PlayTarget>, position: Long, retry: Boolean = true) {
        val requestR = mapOf("uris" to arrayOf(track))
        val reqBody = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(requestR))

        playTargets.map {
            val deviceId = it.device
            val token = it.token
            Thread {
                val url = "https://api.spotify.com/v1/me/player/play" + if (deviceId != null) "device_id=$deviceId" else ""
                val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .put(reqBody)
                        .build()
                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    if (response.code() == 202 && retry) {
//                        return play(track, token, deviceId, offset, false)
                        println("Retry required for ${it}")

                    }

                    response.close()

                    if (position > 0) {
                        seek(position, it, true)
                    }
                } else {
                    response.close()
                    println("Request failed for ${it}")
                }
            }
        }.forEach { it.start() }
    }

    fun seek(position: Long, target: PlayTarget, retry: Boolean = true) {
        val requestR = mapOf("position_ms" to position, "device_id" to target.device)
        val reqBody = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(requestR))

        Thread {
            val url = "https://api.spotify.com/v1/me/player/seek"
            val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${target.token}")
                    .put(reqBody)
                    .build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                if (response.code() == 202 && retry) {
//                        return play(track, token, deviceId, offset, false)
                    println("Retry required")
                }
                response.close()
            } else {
                response.close()
                println("Request failed")
            }
        }.start()
    }


    fun getInfo(token: String) {
        val request = Request.Builder()
                .url("https://api.spotify.com/v1/me/player")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
        val client = OkHttpClient()
        val response = client.newCall(request).execute()
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
                .redirectURI("${Spotify.API_HOST}/callback")
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
                .redirectURI("${Spotify.API_HOST}/callback")
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
                .redirectURI("${Spotify.API_HOST}/callback")
                .build()

        return api.searchArtists(filters.joinToString(" ") { f -> f.compile() })
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }
}

data class PlayTarget(val token: String, val device: String?)

data class SpotifyDevice(val id: String,
                         @field:JsonProperty("is_active") val isActive: Boolean,
                         @field:JsonProperty("is_restricted") val isRestricted: Boolean,
                         val name: String,
                         val type: String,
                         @field:JsonProperty("volume_percent") val volumePercent: Double)

data class SpotifyDeviceResponse(val devices: List<SpotifyDevice>)
