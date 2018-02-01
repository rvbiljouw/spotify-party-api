package uk.bipush.party.bot.spotify

import com.wrapper.spotify.Api
import org.slf4j.LoggerFactory
import uk.bipush.party.bot.BotMother
import uk.bipush.party.model.PartyType

class SpotifyBotMother : BotMother<SpotifyPlaylistBot>(PartyType.SPOTIFY, BOT_EMAIL, BOT_DISPLAY_NAME) {

    val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val API_HOST = System.getenv("API_HOST") ?: "http://localhost:8080"

        val CLIENT_ID = System.getenv("SPOTIFY_CLIENT_ID")
        val CLIENT_SECRET = System.getenv("SPOTIFY_CLIENT_SECRET")

        val BOT_EMAIL = "spotify-playlist-bot@bot.awsum.io"
        val BOT_DISPLAY_NAME = "Spotify Bot"

        val SPOTIFY_CLIENT = Api.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectURI("${API_HOST}/callback")
                .build()
    }

    init {
        refreshToken()
    }

    fun refreshToken() {
        val creds = SPOTIFY_CLIENT.clientCredentialsGrant().build().get()
        SPOTIFY_CLIENT.setAccessToken(creds.accessToken)
    }

    override fun createBot(ownerId: String, playlistId: String): SpotifyPlaylistBot? {
        val spotifyPlaylist = SPOTIFY_CLIENT.getPlaylist(ownerId, playlistId).build().get()

        return if (spotifyPlaylist != null) {
            SpotifyPlaylistBot(spotifyPlaylist, this)
        } else {
            null
        }
    }

    override fun createNewBots(bots: MutableList<SpotifyPlaylistBot>): List<SpotifyPlaylistBot> {
        var offfset: Int = 0

        val limit = 300

        while (true) {
            if (bots.size == limit) {
                break
            }

            val playlists = SPOTIFY_CLIENT.featuredPlaylists.offset(offfset).build().get().playlists

            if (playlists.items.size == 0) {
                break
            }

            val existingBotIds = bots.map { "${it.getPlaylistId()}:${it.getPlaylistOwnerId()}" }.distinct()

            val newBots = playlists.items
                    .filter { !existingBotIds.contains("${it.id}:${it.owner.id}") }
                    .mapNotNull { playlist ->
                        createBot(playlist.owner.id, playlist.id)
                    }

            offfset += playlists.items.size

            bots.addAll(newBots)

            logger.info("Created ${newBots.size} new spotify playlist bots")
        }

        return bots.distinctBy { "${it.getPlaylistId()}:${it.getPlaylistOwnerId()}" }
    }
}