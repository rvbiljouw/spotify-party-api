package uk.bipush.party.bot.youtube

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import org.slf4j.LoggerFactory
import uk.bipush.party.model.Account
import uk.bipush.party.model.AccountType
import java.io.IOException

class YouTubeMaster {

    val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val API_KEY = System.getenv("YOUTUBE_API_KEY")
        val MUSIC_CHANNEL_ID = "UC-9-kyTW8ZkZNDHQJ6FgpwQ"
        val BOT_EMAIL = "yt-playlist-bot@bot.awsum.io"
        val BOT_DISPLAY_NAME = "YouTube Bot"

        private val youtube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), object : HttpRequestInitializer {
            @Throws(IOException::class)
            override fun initialize(request: HttpRequest) {
            }
        }).setApplicationName("awsumio").build()
    }

    fun createBots(): List<YoutubePlaylistBot> {
        var nextToken: String? = null
        val bots: MutableList<YoutubePlaylistBot> = mutableListOf()

        val limit = 1000

        val account = getCreateBotAccount()

        while (true) {
            val req = youtube.playlists().list("id,snippet")
                    .setKey(API_KEY)
                    .setChannelId(MUSIC_CHANNEL_ID)
                    .setMaxResults(50)

            if (nextToken != null) {
                req.pageToken = nextToken
            }

            val resp = req.execute()

            nextToken = resp.nextPageToken

            val newBots = resp.items.map { playlist ->
                YoutubePlaylistBot(
                        playlist,
                        account
                )
            }

            bots.addAll(newBots)

            logger.info("Created ${newBots.size} new youtube playlist bots")

            if (bots.size == limit) {
                break
            }

            if (nextToken == null) {
                break
            }
        }

        return bots
    }

    private fun getCreateBotAccount(): Account {
        var account = Account.finder.query().where()
                .eq("email", BOT_EMAIL)
                .eq("accountType", AccountType.BOT)
                .findOne()

        if (account == null) {
            account = Account().apply {
                this.email = BOT_EMAIL
                this.accountType = AccountType.BOT
                this.displayName = BOT_DISPLAY_NAME
            }

            account.save()
        }

        return account
    }
}