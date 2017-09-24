package uk.bipush.party.endpoint

import com.wrapper.spotify.Api
import io.ebean.Expr
import spark.Route
import spark.Spark
import uk.bipush.party.model.Account
import uk.bipush.party.util.JacksonResponseTransformer
import uk.bipush.party.util.Spotify

/**
 * @author rvbiljouw
 */
class LoginEndpoint : Endpoint {

    private val login = Route { req, res ->
        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("http://localhost:8080/login/callback")
                .build()
        val scopes = listOf<String>("streaming")
        val authorizeURL = api.createAuthorizeURL(scopes, "secret")

        res.redirect(authorizeURL)
    }

    private val callback = Route { req, res ->
        val state = req.queryParams("state")
        val code = req.queryParams("code")

        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("http://localhost:8080/login/callback")
                .build()
        val credentials = api.authorizationCodeGrant(code).build().get()

        val spotifyUser = api.me.accessToken(credentials.accessToken).build().get()
        when {
            spotifyUser == null -> {
                res.status(400)
                mapOf("success" to false, "error" to "Couldn't retrieve user account.")
            }
            else -> {
                var account = Account.finder.query().where(Expr.eq("spotifyId", spotifyUser.id)).findOne()
                if (account == null) {
                    account = Account().apply {
                        this.accessToken = credentials.accessToken
                        this.refreshToken = credentials.refreshToken
                        this.spotifyId = spotifyUser.id
                        this.displayName = spotifyUser.displayName
                    }
                } else {
                    account.accessToken = credentials.accessToken
                    account.refreshToken = credentials.refreshToken
                }

                account.save()

                req.session(true).attribute("user_id", account.id)
                account
            }
        }
    }

    override fun init() {
        Spark.get("/login", login)
        Spark.get("/login/callback", callback, JacksonResponseTransformer())
    }

}