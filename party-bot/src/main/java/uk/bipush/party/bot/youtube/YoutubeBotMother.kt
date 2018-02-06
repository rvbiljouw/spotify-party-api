package uk.bipush.party.bot.youtube

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import io.ebean.Expr
import org.slf4j.LoggerFactory
import uk.bipush.party.bot.BotMother
import uk.bipush.party.model.Account
import uk.bipush.party.model.AccountType
import uk.bipush.party.model.PartyType
import uk.bipush.party.model.PlaylistParty
import uk.bipush.party.util.DBUtils
import java.io.IOException

class YoutubeBotMother: BotMother<YoutubePlaylistBot>(PartyType.YOUTUBE, BOT_EMAIL, BOT_DISPLAY_NAME) {

    val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val API_KEY = System.getenv("YOUTUBE_API_KEY")
        val MUSIC_CHANNEL_ID = "UC-9-kyTW8ZkZNDHQJ6FgpwQ"
        val BOT_EMAIL = "yt-playlist-bot@bot.awsum.io"
        val BOT_DISPLAY_NAME = "YouTube Bot"

        val BOT_LIMIT = System.getenv("YOUTUBE_BOTS")?.toInt() ?: 50

        private val youtube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), object : HttpRequestInitializer {
            @Throws(IOException::class)
            override fun initialize(request: HttpRequest) {
            }
        }).setApplicationName("awsumio").build()
    }

    override fun createBot(ownerId: String, playlistId: String): YoutubePlaylistBot? {
        val resp = youtube.playlists().list("id,snippet")
                .setKey(API_KEY)
                .setId(playlistId)
                .execute()

        return if (resp.items.isNotEmpty()) {
            YoutubePlaylistBot(resp.items[0], this)
        } else {
            null
        }
    }

    override fun createNewBots(bots: MutableList<YoutubePlaylistBot>): List<YoutubePlaylistBot> {
        var nextToken: String? = null

        val account = getCreateBotAccount()

        while (true) {
            if (bots.size == BOT_LIMIT) {
                break
            }

            val req = youtube.playlists().list("id,snippet")
                    .setKey(API_KEY)
                    .setChannelId(MUSIC_CHANNEL_ID)
                    .setMaxResults(50)

            if (nextToken != null) {
                req.pageToken = nextToken
            }

            val resp = req.execute()

            if (resp.items.size == 0) {
                break
            }

            nextToken = resp.nextPageToken

            val existingBotIds = bots.map { "${it.getPlaylistId()}:${it.getPlaylistOwnerId()}" }.distinct()

            val newBots = resp.items
                    .filter {!existingBotIds.contains("${it.id}:${it.snippet.channelId}") }
                    .map { playlist ->
                YoutubePlaylistBot(
                        playlist,
                        this
                )
            }

            bots.addAll(newBots)

            logger.info("Created ${newBots.size} new youtube playlist bots")

            if (nextToken == null) {
                break
            }
        }

        return bots.distinctBy { "${it.getPlaylistId()}:${it.getPlaylistOwnerId()}" }
    }
}