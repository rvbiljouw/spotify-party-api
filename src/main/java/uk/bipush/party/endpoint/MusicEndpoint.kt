package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import spark.Route
import spark.Spark
import uk.bipush.party.endpoint.response.fromSpotify
import uk.bipush.party.model.Account
import uk.bipush.party.util.JacksonResponseTransformer
import uk.bipush.party.util.Spotify
import uk.bipush.party.util.SpotifyFilter

class MusicEndpoint : Endpoint {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    override fun init() {
        Spark.post("/api/v1/music/songs", searchSongs, JacksonResponseTransformer())
        Spark.post("/api/v1/music/albums", searchAlbums, JacksonResponseTransformer())
        Spark.post("/api/v1/music/artists", searchArtists, JacksonResponseTransformer())
    }

    val searchSongs = Route { req, res ->
        val filters: List<SpotifyFilter> = mapper.readValue(req.body())
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val userId: Long? = req.session().attribute("user_id")
        if (userId != null) {
            val account = Account.finder.byId(userId)
            if (account != null) {
                val songs = Spotify.searchSongs(account.accessToken!!, account.refreshToken!!, filters, offset, limit)

                if (songs != null) {
                    res.header("X-Max-Records", songs.total.toString())
                    res.header("X-Offset", (offset).toString())

                    songs.items.map { t -> t.fromSpotify(true) }
                } else {
                    res.status(500)
                    mapOf("error" to "Unable to get songs.")
                }
            } else {
                res.status(403)
                mapOf("error" to "You're not logged in.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val searchAlbums = Route { req, res ->
        val filters: List<SpotifyFilter> = mapper.readValue(req.body())
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val userId: Long? = req.session().attribute("user_id")
        if (userId != null) {
            val account = Account.finder.byId(userId)
            if (account != null) {
                val albums = Spotify.searchAlbums(account.accessToken!!, account.refreshToken!!, filters, offset, limit)

                if (albums != null) {
                    res.header("X-Max-Records", albums.total.toString())
                    res.header("X-Offset", (offset).toString())

                    albums.items.map { t -> t.fromSpotify() }
                } else {
                    res.status(500)
                    mapOf("error" to "Unable to get albums.")
                }
            } else {
                res.status(403)
                mapOf("error" to "You're not logged in.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val searchArtists = Route { req, res ->
        val filters: List<SpotifyFilter> = mapper.readValue(req.body())
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val userId: Long? = req.session().attribute("user_id")
        if (userId != null) {
            val account = Account.finder.byId(userId)
            if (account != null) {
                val artists = Spotify.searchArtists(account.accessToken!!, account.refreshToken!!, filters, offset, limit)

                if (artists != null) {
                    res.header("X-Max-Records", artists.total.toString())
                    res.header("X-Offset", (offset).toString())

                    artists.items.map { t -> t.fromSpotify() }
                } else {
                    res.status(500)
                    mapOf("error" to "Unable to get artists.")
                }
            } else {
                res.status(403)
                mapOf("error" to "You're not logged in.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }


}