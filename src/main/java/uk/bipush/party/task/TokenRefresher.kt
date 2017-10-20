package uk.bipush.party.task

import com.wrapper.spotify.Api
import uk.bipush.party.model.Account
import uk.bipush.party.util.Spotify

class TokenRefresher: Runnable {

    companion object {
        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("${Spotify.API_HOST}/callback")
                .build()
    }

    override fun run() {
        val tokens = Account.finder.query()
                .setFirstRow(0)
                .setMaxRows(25)
                .findPagedList()
        tokens.loadCount()

        val count = tokens.totalCount
        var offset = 0
        while (offset < count) {
            val results = Account.finder.query()
                    .setFirstRow(offset)
                    .setMaxRows(25)
                    .findPagedList()
            results.list.forEach({ acc ->
                api.setAccessToken(acc.accessToken)
                api.setRefreshToken(acc.refreshToken)
                val newToken = api.refreshAccessToken()
                        .refreshToken(acc.refreshToken)
                        .build().get()
                acc.accessToken = newToken.accessToken
                acc.update()
                println("Updated ${acc.displayName}")
            })
            offset += results.list.size
        }
    }

}