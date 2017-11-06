package uk.bipush.party.task

import com.wrapper.spotify.Api
import org.slf4j.LoggerFactory
import uk.bipush.party.model.Account
import uk.bipush.party.model.AccountType
import uk.bipush.party.util.Spotify

class TokenRefresher : Runnable {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("${Spotify.API_HOST}/callback")
                .build()
    }

    override fun run() {
        Spotify.refreshToken()

        val tokens = Account.finder.query()
                .setFirstRow(0)
                .setMaxRows(25)
                .findPagedList()
        tokens.loadCount()

        val count = tokens.totalCount
        var offset = 0
        while (offset < count) {
            val results = Account.finder.query()
                    .where().isNotNull("spotify")
                    .setFirstRow(offset)
                    .setMaxRows(25)
                    .findPagedList()
            results.list.forEach({ acc ->
                api.setAccessToken(acc.spotify?.accessToken)
                api.setRefreshToken(acc.spotify?.refreshToken)
                val newToken = api.refreshAccessToken()
                        .refreshToken(acc.spotify?.refreshToken)
                        .grantType("refresh_token")
                        .build().get()
                acc.spotify?.accessToken = newToken.accessToken
                acc.spotify?.update()
            })
            offset += results.list.size
        }
    }

}