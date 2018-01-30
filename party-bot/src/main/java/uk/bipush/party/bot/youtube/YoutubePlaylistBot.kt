package uk.bipush.party.bot.youtube

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Playlist
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import uk.bipush.party.api.CreatePartyRequest
import uk.bipush.party.api.PartyApi
import uk.bipush.party.api.QueueSongRequest
import uk.bipush.party.bot.Bot
import uk.bipush.party.model.*
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class YoutubePlaylistBot(val playlist: Playlist, val account: Account) : Bot() {

    companion object {
        val API_KEY = System.getenv("YOUTUBE_API_KEY")

        private val youtube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), object : HttpRequestInitializer {
            @Throws(IOException::class)
            override fun initialize(request: HttpRequest) {
            }
        }).setApplicationName("awsumio").build()

        private val durationCache: LoadingCache<String, Long> = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .maximumSize(4096)
                .build(object : CacheLoader<String, Long>() {
                    override fun load(p0: String?): Long {
                        val details = youtube.videos()
                                .list("contentDetails")
                                .setId(p0)
                                .setKey(API_KEY)
                                .execute()
                        val video = details.items.firstOrNull()
                        if (video != null) {
                            return Duration.parse(video.contentDetails.duration).toMillis()
                        } else {
                            throw IllegalArgumentException("Video doesn't exist.")
                        }
                    }
                })
    }

    override fun getBotAccount(): Account {
        return account
    }

    override fun getBotParty(): Party {
        var party = Party.finder.query().where()
                .eq("name", playlist.snippet.title)
                .eq("owner.accountType", AccountType.BOT)
                .findOne()

        val token = getToken()

        if (party == null) {
            party = PartyApi.createParty(
                    token.token!!,
                    CreatePartyRequest(
                            PartyType.YOUTUBE,
                            PartyAccess.PUBLIC,
                            playlist.snippet.title, playlist.snippet.description
                    )
            )
        }

        return party
    }

    override fun getNextSongs(): Set<QueueSongRequest> {
        var nextToken: String? = null

        val party = getBotParty()

        val requests: MutableList<QueueSongRequest> = mutableListOf()

        while (true) {
            val req = youtube.playlistItems().list("snippet,contentDetails")
                    .setPlaylistId(playlist.id)
                    .setKey(API_KEY)
                    .setMaxResults(50)

            if (nextToken != null) {
                req.pageToken = nextToken
            }

            val resp = req.execute()

            nextToken = resp.nextPageToken

            requests.addAll(resp.items.mapNotNull { item ->
                val id = item.snippet.resourceId.videoId
                val videoTitle = item.snippet.title.split("-")
                val artist = videoTitle[0]
                var title = ""
                if (videoTitle.size > 1) {
                    title = videoTitle[1]
                }
                val thumbnail = item.snippet.thumbnails.high.url
                val url = "https://www.youtube.com/watch?v=$id"
                val duration = durationCache.get(id) ?: 0

                val entries = PartyQueueEntry.finder.query().where()
                        .eq("party.id", party.id)
                        .eq("uri", url)
                        .setMaxRows(1)
                        .findList()

                if (entries.isEmpty()) {
                    QueueSongRequest(artist, title, thumbnail, url, duration.toInt())
                } else null
            })

            if (nextToken == null) {
                break
            }

        }

        return requests.distinctBy { it.uri }.toSet()
    }
}