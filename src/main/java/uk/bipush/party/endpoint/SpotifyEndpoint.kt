package uk.bipush.party.endpoint

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.wrapper.spotify.Api
import com.wrapper.spotify.models.AuthorizationCodeCredentials
import io.ebean.Expr
import org.mindrot.jbcrypt.BCrypt
import spark.Route
import spark.route.HttpMethod
import uk.bipush.http.Endpoint
import uk.bipush.http.auth.Auth
import uk.bipush.http.response.ErrorResponse
import uk.bipush.http.response.Errors
import uk.bipush.http.response.error
import uk.bipush.http.response.response
import uk.bipush.http.util.ValidatedRequest
import uk.bipush.http.util.validate
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.endpoint.response.fromSpotify
import uk.bipush.party.endpoint.response.response
import uk.bipush.party.handler.PartyManager
import uk.bipush.party.model.*
import uk.bipush.party.util.Spotify
import uk.bipush.party.util.SpotifyFilter
import java.util.*
import java.util.concurrent.TimeUnit

class SpotifyEndpoint {

    companion object {
        private val authCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5L, TimeUnit.MINUTES)
                .build<String, AuthorizationCodeCredentials>()
        private val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("${Spotify.API_HOST}/callback")
                .build()
        private val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/spotify/login")
    private val login = Route { req, res ->
        val redirectUrl = req.queryParams("redirectUrl")
        val scopes = listOf<String>("user-modify-playback-state", "user-read-playback-state")
        val authorizeURL = api.createAuthorizeURL(scopes, redirectUrl)

        res.redirect(authorizeURL)
    }

    @field:Endpoint(method = HttpMethod.get, uri = "/callback")
    private val callback = Route { req, res ->
        try {
            val state = req.queryParams("state")
            val code = req.queryParams("code")

            var credentials = authCache.getIfPresent(code)
            if (credentials == null) {
                val api = Api.builder()
                        .clientId(Spotify.CLIENT_ID)
                        .clientSecret(Spotify.CLIENT_SECRET)
                        .redirectURI("${Spotify.API_HOST}/callback")
                        .build()
                credentials = api.authorizationCodeGrant(code).build().get()
                authCache.put(code, credentials)
            }

            val spotifyUser = api.me.accessToken(credentials?.accessToken).build().get()
            when {
                credentials == null -> {
                    res.status(400)
                    mapOf("success" to false, "error" to "Couldn't retrieve token.")
                }
                spotifyUser == null -> {
                    res.status(400)
                    mapOf("success" to false, "error" to "Couldn't retrieve user account.")
                }
                else -> {
                    var account = Account.finder.query().where(Expr.eq("spotify.spotifyId", spotifyUser.id)).findOne()
                    if (account == null) {
                        // We're creating a new account using the Spotify account as a base
                        account = Account().apply {
                            this.displayName = if (spotifyUser.displayName?.isNotBlank() == true)
                                spotifyUser.displayName
                            else
                                spotifyUser.id
                            this.email = spotifyUser.email
                        }
                        account.save()

                        val devices = Spotify.getDevices(credentials.accessToken)
                        val deviceId = if (devices != null && devices.devices.isNotEmpty())
                            (devices.devices.find { it.isActive } ?: devices.devices[0]).id
                        else
                            null

                        val spotifyAcc = SpotifyAccount().apply {
                            this.account = account
                            this.accessToken = credentials?.accessToken
                            this.refreshToken = credentials?.refreshToken
                            this.displayName = spotifyUser.displayName
                            this.spotifyId = spotifyUser.id
                            this.device = deviceId
                        }
                        spotifyAcc.save()

                        account.spotify = spotifyAcc
                        account.save()


                    } else {
                        if (account.spotify != null) {
                            account.spotify?.accessToken = credentials.accessToken
                            account.spotify?.refreshToken = credentials.refreshToken
                        } else {
                            val spotifyAcc = SpotifyAccount().apply {
                                this.account = account
                                this.accessToken = credentials?.accessToken
                                this.refreshToken = credentials?.refreshToken
                                this.displayName = spotifyUser.displayName
                                this.spotifyId = spotifyUser.id
                            }
                            spotifyAcc.save()

                            account.spotify = spotifyAcc
                        }
                    }

                    if (account.loginToken == null) {
                        account.loginToken = LoginToken().apply {
                            this.account = account
                            this.userAgent = req.userAgent()
                            this.ipAddress = req.ip()
                            this.status = LoginTokenStatus.ACTIVE
                            this.token = UUID.randomUUID().toString()
                        }
                        account.loginToken?.save()
                    }

                    account.save()

                    req.session(true).attribute("token", account.loginToken?.token)

                    if (state?.isNotBlank() == true) {
                        res.redirect("$state?loginToken=${account.loginToken?.token}")
                    } else {
                        res.redirect(Spotify.FRONTEND_HOST)
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            val exId = UUID.randomUUID().toString()

            res.status(500)
            mapOf("error" to "An error occurred - $exId")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/spotify/devices")
    private val getDevices = Route { req, res ->
        val token: LoginToken = req.attribute("account")

        if (token.account?.spotify?.accessToken == null) {
            res.error(Errors.notFound, ErrorResponse("You haven't linked a spotify account"))
        } else {
            val devices = Spotify.getDevices(token.account?.spotify?.accessToken!!)

            devices?.devices?.map {
                it.response()
            }
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.put, uri = "/api/v1/spotify/account")
    val updateAccount = Route { req, res ->
        val token: LoginToken = req.attribute("account")

        val updateRequest: UpdateSpotifyAccountRequest = mapper.readValue(req.body())

        val errors = updateRequest.validate()
        if (errors.isEmpty()) {
            if (token.account?.spotify == null) {
                res.error(Errors.notFound, ErrorResponse("You haven't linked a spotify account"))
            } else {
                val spotifyAccount = token.account!!.spotify!!

                val changedDevice = updateRequest.device?.isNotBlank() == true && updateRequest.device != spotifyAccount.device
                if (changedDevice) {
                    spotifyAccount.device = updateRequest.device
                }

                spotifyAccount.update()

                if (changedDevice && spotifyAccount.activeParty != null) {
                    val partyMember = PartyMember.finder.query().where()
                            .eq("party.id", spotifyAccount.activeParty!!.id)
                            .eq("account.id", token.account!!.id)
                            .findUnique()

                    PartyManager.managers[spotifyAccount.activeParty!!.type]?.onMemberAdded(partyMember!!, false)
                }

                spotifyAccount.response(false, false)
            }
        } else {
            res.error(Errors.badRequest, errors.map { it.response() })
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/spotify/songs")
    val searchSongs = Route { req, res ->
        val filters: List<SpotifyFilter> = mapper.readValue(req.body())
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val token: LoginToken = req.attribute("account")

        println("spotif token ${token.account?.spotify}")
        val account = token.account?.spotify
        if (account != null) {
            val songs = Spotify.searchSongs(account.accessToken!!, account.refreshToken!!, filters, offset, limit)

            if (songs != null) {
                res.header("X-Max-Records", songs.total.toString())
                res.header("X-Offset", (offset).toString())

                songs.items.map { t -> t.fromSpotify() }
            } else {
                res.status(500)
                mapOf("error" to "Unable to get songs.")
            }
        } else {
            res.status(403)
            mapOf("error" to "Your Spotify account isn't linked.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/spotify/albums")
    val searchAlbums = Route { req, res ->
        val filters: List<SpotifyFilter> = mapper.readValue(req.body())
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val token: LoginToken = req.attribute("account")
        val account = token.account?.spotify
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
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/spotify/artists")
    val searchArtists = Route { req, res ->
        val filters: List<SpotifyFilter> = mapper.readValue(req.body())
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val token: LoginToken = req.attribute("account")
        val account = token.account?.spotify
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
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateSpotifyAccountRequest(val device: String?) : ValidatedRequest()