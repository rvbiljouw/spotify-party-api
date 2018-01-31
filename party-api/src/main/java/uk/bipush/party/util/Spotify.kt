package uk.bipush.party.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.wrapper.spotify.Api
import com.wrapper.spotify.models.Artist
import com.wrapper.spotify.models.Page
import com.wrapper.spotify.models.SimpleAlbum
import com.wrapper.spotify.models.Track
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.eclipse.jetty.http.HttpStatus
import org.slf4j.LoggerFactory
import uk.bipush.party.rmq.InvalidDevicePublisher
import uk.bipush.party.rmq.NonPremiumSpotifyPublisher
import java.util.*


object Spotify {
    val CLIENT_ID = System.getenv("SPOTIFY_CLIENT_ID")
    val CLIENT_SECRET = System.getenv("SPOTIFY_CLIENT_SECRET")
    val API_HOST = System.getenv("API_HOST") ?: "http://localhost:8080"
    val FRONTEND_HOST = System.getenv("FRONTEND_HOST") ?: "http://localhost:4200"

    val mapper = ObjectMapper().registerModule(KotlinModule())

    val logger = LoggerFactory.getLogger(Spotify::class.java)

    val timer = Timer()
    val api: Api

    init {
        api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("${Spotify.API_HOST}/callback")
                .build()
        refreshToken()
    }

    fun refreshToken() {
        val creds = api.clientCredentialsGrant().build().get()
        api.setAccessToken(creds.accessToken)
    }

    fun play(track: String, playTargets: List<PlayTarget>, position: Long, retry: Boolean = true) {
        val requestR = mapOf("uris" to arrayOf(track))
        val reqBody = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(requestR))

        playTargets.map {
            val deviceId = it.device
            val token = it.token
            Thread {
                var successful: Boolean = false
                var cycleCount = 0
                while (!successful) {
                    val url = "https://api.spotify.com/v1/me/player/play" + if (deviceId != null) "?device_id=$deviceId" else ""

                    val request = Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $token")
                            .put(reqBody)
                            .build()
                    val client = OkHttpClient()
                    val response = client.newCall(request).execute()

                    when (response.code()) {
                        HttpStatus.NO_CONTENT_204 -> {
                            if (position > 0) {
                                seek(position, it, true)
                            }
                            successful = true
                        }
                        HttpStatus.ACCEPTED_202 -> {
                            logger.info("Retry required for $it [$response]")
                            successful = !retry
                        }
                        HttpStatus.NOT_FOUND_404 -> {
                            if (it.device != null) {
                                InvalidDevicePublisher.publishInvalidDevice(it.accountId, it.device)
                            }
                            logger.warn("Request failed for $it [$response]")
                            successful = true
                        }
                        HttpStatus.FORBIDDEN_403 -> {
                            NonPremiumSpotifyPublisher.publishNonPremium(it.accountId)
                            logger.warn("Request failed for $it [$response]")
                            successful = true
                        }
                        else -> {
                            logger.warn("Unandled response [$response]")
                            successful = true
                        }
                    }
                    response.close()

                    cycleCount++

                    if (cycleCount > 5) {
                        return@Thread
                    }

                    Thread.sleep(200)
                }
            }
        }.forEach { it.start() }
    }

    fun pause(playTargets: List<PlayTarget>, retry: Boolean = true) {
        val reqBody = RequestBody.create(MediaType.parse("application/json"), "")

        playTargets.map {
            val deviceId = it.device
            val token = it.token
            Thread {
                var successful: Boolean = false
                var cycleCount = 0
                while (!successful) {
                    val url = "https://api.spotify.com/v1/me/player/pause" + if (deviceId != null) "?device_id=$deviceId" else ""

                    val request = Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $token")
                            .put(reqBody)
                            .build()
                    val client = OkHttpClient()
                    val response = client.newCall(request).execute()

                    when (response.code()) {
                        HttpStatus.NO_CONTENT_204 -> {
                            successful = true
                        }
                        HttpStatus.ACCEPTED_202 -> {
                            logger.info("Retry required for $it [$response]")
                            successful = !retry
                        }
                        HttpStatus.NOT_FOUND_404 -> {
                            if (it.device != null) {
                                InvalidDevicePublisher.publishInvalidDevice(it.accountId, it.device)
                            }
                            logger.warn("Request failed for $it [$response]")
                            successful = true
                        }
                        HttpStatus.FORBIDDEN_403 -> {
                            NonPremiumSpotifyPublisher.publishNonPremium(it.accountId)
                            logger.warn("Request failed for $it [$response]")
                            successful = true
                        }
                        else -> {
                            logger.warn("Unandled response [$response]")
                            successful = true
                        }
                    }
                    response.close()

                    cycleCount++

                    if (cycleCount > 5) {
                        return@Thread
                    }

                    Thread.sleep(200)
                }
            }
        }.forEach { it.start() }
    }

    fun seek(position: Long, target: PlayTarget, retry: Boolean = true) {
        val requestR = mapOf("position_ms" to position, "device_id" to target.device)
        val reqBody = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(requestR))

        Thread {
            val url = "https://api.spotify.com/v1/me/player/seek?position_ms=$position" + if (target.device != null) "&device_id=${target.device}" else ""
            val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${target.token}")
                    .put(reqBody)
                    .build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                if (response.code() == 202 && retry) {
                    logger.info("Retry required")
                }
                response.close()
            } else {
                logger.warn("Request failed [$response]")
                response.close()
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
        if (response.isSuccessful && response.body() != null) {
            val msg = response.body()!!.string()
            val devices: SpotifyDeviceResponse = mapper.readValue(msg)
            response.close()
            return devices
        } else {
            return null
        }
    }

    fun searchSongs(accessToken: String, refreshToken: String, filters: List<SpotifyFilter>, offset: Int = 0, limit: Int = 25): Page<Track>? {
        return api.searchTracks(filters.joinToString("+") { f -> f.compile() })
                .market("GB")
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }


    fun searchSongsByQueryM(query: String, offset: Int = 0, limit: Int = 25): Page<Track>? {
        return api.searchTracks(query.replace(" ", "+"))
                .market("GB")
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }

    fun searchSongsByQuery(accessToken: String, refreshToken: String, query: String, offset: Int = 0, limit: Int = 25): Page<Track>? {
        return api.searchTracks(query.replace(" ", "+"))
                .market("GB")
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }

    fun searchAlbums(accessToken: String, refreshToken: String, filters: List<SpotifyFilter>, offset: Int = 0, limit: Int = 25): Page<SimpleAlbum>? {
        return api.searchAlbums(filters.joinToString(" ") { f -> f.compile() })
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }

    fun searchArtists(accessToken: String, refreshToken: String, filters: List<SpotifyFilter>, offset: Int = 0, limit: Int = 25): Page<Artist>? {
        return api.searchArtists(filters.joinToString(" ") { f -> f.compile() })
                .offset(offset)
                .limit(limit)
                .build()
                .get()
    }
}

data class PlayTarget(val accountId: Long, val token: String, val device: String?)

data class SpotifyDevice(val id: String,
                         @field:JsonProperty("is_active") val isActive: Boolean,
                         @field:JsonProperty("is_restricted") val isRestricted: Boolean,
                         val name: String,
                         val type: String,
                         @field:JsonProperty("volume_percent") val volumePercent: Double)

data class SpotifyDeviceResponse(val devices: List<SpotifyDevice>)
