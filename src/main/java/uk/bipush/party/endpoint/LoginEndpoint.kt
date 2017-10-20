package uk.bipush.party.endpoint

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.wrapper.spotify.Api
import com.wrapper.spotify.models.AuthorizationCodeCredentials
import io.ebean.Expr
import spark.Route
import spark.Spark
import uk.bipush.party.model.Account
import uk.bipush.party.util.JacksonResponseTransformer
import uk.bipush.party.util.Spotify
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author rvbiljouw
 */
class LoginEndpoint : Endpoint {
    private val authCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build<String, AuthorizationCodeCredentials>()

    companion object {
        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("${Spotify.API_HOST}/callback")
                .build()
    }

    private val login = Route { req, res ->
        val scopes = listOf<String>("user-modify-playback-state", "user-read-playback-state")
        val authorizeURL = api.createAuthorizeURL(scopes, "secret")

        println(authorizeURL)
        res.redirect(authorizeURL)
    }

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
                    var account = Account.finder.query().where(Expr.eq("spotifyId", spotifyUser.id)).findOne()
                    if (account == null) {
                        account = Account().apply {
                            this.accessToken = credentials!!.accessToken
                            this.refreshToken = credentials!!.refreshToken
                            this.spotifyId = spotifyUser.id
                            this.displayName = spotifyUser.displayName
                        }
                    } else {
                        account.accessToken = credentials.accessToken
                        account.refreshToken = credentials.refreshToken
                    }

                    account.save()

                    req.session(true).attribute("user_id", account.id)
                    res.redirect("http://localhost:4200")
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            val exId = UUID.randomUUID().toString()
            println("$exId: ${t.toString()}")
            res.status(500)
            mapOf("error" to "An error occurred - $exId")
        }
    }

    override fun init() {
        Spark.get("/api/v1/login", login)
        Spark.get("/callback", callback, JacksonResponseTransformer())
    }

}