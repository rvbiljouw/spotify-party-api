package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import spark.Route
import spark.route.HttpMethod
import uk.bipush.http.Endpoint
import uk.bipush.http.auth.Auth
import uk.bipush.party.endpoint.response.TrackResponse
import uk.bipush.party.model.LoginToken
import uk.bipush.party.task.YouTubeBot
import uk.bipush.party.util.SpotifyFilter
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

class YouTubeEndpoint {

    companion object {
        private val youtube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), object : HttpRequestInitializer {
            @Throws(IOException::class)
            override fun initialize(request: HttpRequest) {
            }
        }).setApplicationName("awsumio").build()
        private val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        private val durationCache: LoadingCache<String, Long> = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .maximumSize(4096)
                .build(object : CacheLoader<String, Long>() {
                    override fun load(p0: String?): Long {
                        val details = youtube.videos()
                                .list("contentDetails")
                                .setId(p0)
                                .setKey(YouTubeBot.API_KEY)
                                .execute()
                        val video = details.items.firstOrNull()
                        if (video != null) {
                            return Duration.parse(video.contentDetails.duration).toMillis()
                        } else {
                            throw IllegalArgumentException("Video doesn't exist.")
                        }
                    }
                })
        private val pageTokenCache: Cache<String, String> = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build()
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/youtube/songs")
    val searchSongs = Route { req, res ->
        val filters: List<SpotifyFilter> = mapper.readValue(req.body())
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val token: LoginToken = req.attribute("account")

        val search = youtube.search().list("id,snippet")
        if (offset > 0) {
            search.pageToken = pageTokenCache.getIfPresent("${req.body()}_page_${offset / limit}")
            println("used existng page token")
        }
        search.key = YouTubeBot.API_KEY
        search.q = filters.joinToString(" ") { f -> f.value!! }
        search.videoCategoryId = "10"
        search.type = "video"
        search.videoDuration = "short"
        search.maxResults = limit.toLong()
        search.order = "relevance"
        val result = search.execute()

        res.header("X-Max-Records", result.pageInfo.totalResults.toString())
        res.header("X-Offset", (offset).toString())

        if (result.nextPageToken != null) {
            pageTokenCache.put("${req.body()}_page_${(offset + limit) / limit}", result.nextPageToken)
        }
        if (result.prevPageToken != null) {
            pageTokenCache.put("${req.body()}_page_${(offset - limit) / limit}", result.prevPageToken)
        }

        result.items.map {
            val id = it.id.videoId
            val videoTitle = it.snippet.title.split("-")
            val artist = videoTitle[0]
            var title = ""
            if (videoTitle.size > 1) {
                title = videoTitle[1]
            }
            val thumbnail = it.snippet.thumbnails.default.url
            val url = "https://www.youtube.com/watch?v=$id"
            val duration = durationCache.get(id) ?: 0
            TrackResponse().apply {
                this.id = id
                this.artist = artist
                this.title = title
                this.uri = url
                this.thumbnail = thumbnail
                this.duration = duration.toInt()
            }
        }
    }


}
