package uk.bipush.party.task

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import uk.bipush.party.model.PartyType
import java.io.IOException
import java.time.Duration


class YouTubeBot : PartyBot(PartyType.YOUTUBE) {

    companion object {
        val API_KEY = "AIzaSyBgXbYKZu6LFpJ7J8bbC-FRLsPhGOflUrk"
        val youtube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), { })
                .setApplicationName("awsumio").build()
        val botChannels: List<BotChannel> = listOf(
                BotChannel(name = "ASOT YT", description = "The best of Trance.", keywords = listOf(
                        "armin van buuren",
                        "aly fila",
                        "cosmic gate",
                        "ferry corsten"
                )),
                BotChannel(name = "NL HIPHOP", description = "ayy", keywords = listOf(
                        "fresku",
                        "jiggy dje",
                        "ares"
                ))
        )
    }

    override fun getChannels(): List<BotChannel> {
        return botChannels
    }

    override fun getTracks(keyword: String): List<Song> {
        val search = youtube.search().list("id,snippet")
        search.key = API_KEY
        search.q = keyword
        search.type = "video"
        search.videoDuration = "short"
        search.maxResults = 25

        val result = search.execute()
        return result.items.map {
            val id = it.id.videoId
            val videoTitle = it.snippet.title.split("-")
            val artist = videoTitle[0]
            var title = ""
            if (videoTitle.size > 1) {
                title = videoTitle[1]
            }
            val thumbnail = it.snippet.thumbnails.default.url
            val url = "https://www.youtube.com/watch?v=$id"

            val details = youtube.videos()
                    .list("contentDetails")
                    .setId(id)
                    .setKey(API_KEY)
                    .execute()
            val video = details.items.firstOrNull()
            if (video != null) {
                val duration = Duration.parse(video.contentDetails.duration)
                Song(title, artist, duration.toMillis().toInt(), thumbnail, url)
            } else {
                null
            }
        }.filterNotNull()
    }

}