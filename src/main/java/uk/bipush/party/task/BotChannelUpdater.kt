package uk.bipush.party.task

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.wrapper.spotify.Api
import com.wrapper.spotify.models.Track
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.bipush.party.endpoint.QueueSongRequest
import uk.bipush.party.handler.PartyHandler
import uk.bipush.party.model.Account
import uk.bipush.party.model.AccountType
import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyAccess
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.util.Spotify
import java.util.*
import java.util.stream.IntStream.range
import kotlin.streams.toList

class BotChannelUpdater(val partyHandler: PartyHandler) : Runnable {
    val mapper = ObjectMapper().registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val botChannels: List<BotChannel> = listOf(
            BotChannel(name = "ASOT", description = "ASOT", keywords = listOf(
                    "armin van buuren",
                    "aly fila",
                    "cosmic gate",
                    "ferry corsten"
            )),
            BotChannel(name = "Ed Sheriff", description = "Omg", keywords = listOf(
                    "ed sheeran",
                    "plan b"
            ))
    )
    val api: Api

    init {
        api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("${Spotify.API_HOST}/callback")
                .build()
        val creds = api.clientCredentialsGrant().build().get()
        api.setAccessToken(creds.accessToken)
    }

    override fun run() {
        var botUser = Account.finder.query().where().eq("displayName", "Scotty").findUnique()
        if (botUser == null) {
            botUser = Account().apply {
                this.accountType = AccountType.BOT
                this.displayName = "Scotty"
            }
            botUser.save()
        }

        botChannels.forEach { channel ->
            var party = Party.finder.query().where()
                    .eq("name", channel.name)
                    .eq("description", channel.description)
                    .findUnique()
            if (party == null) {
                party = Party().apply {
                    this.owner = botUser
                    this.name = channel.name
                    this.description = channel.description
                    this.access = PartyAccess.PUBLIC
                }
                party.save()
                partyHandler.addParty(party.id)
            }

            val queue = PartyQueue.forParty(party, 0, 25)
            if (queue.entries.size < 1000) {
                if (channel.urlFeed != null) {
                    val request = Request.Builder()
                            .url(channel.urlFeed)
                            .get().build()
                    val client = OkHttpClient()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val requests: List<QueueSongRequest> = mapper.readValue(response.body()!!.string())
                        requests.forEach { songReq ->
                            PartyQueue.queueSong(botUser!!, party!!, songReq.title, songReq.artist,
                                    songReq.duration, songReq.thumbnail, songReq.uri)
                        }
                    }
                } else {
                    val random = Random()
                    val masterList: MutableList<ComparableSong> = mutableListOf()
                    channel.keywords.forEach {
                        val results = Spotify.searchSongsByQueryM(api, it, 0, 10)
                        if (results != null && results.items.isNotEmpty()) {
                            val songs = range(0, results.items.size - 1)
                                    .map { random.nextInt(results.items.size - 1) }
                                    .toList().toHashSet()
                                    .map { ComparableSong(results.items[it]) }
                                    .sortedByDescending { it.track.popularity }
                                    .filter { track -> queue.entries.count { it.uri == track.track.uri } == 0 }
                                    .take(5)
                                    .toSet()
                            masterList.addAll(songs)
                        }
                    }

                    for (i in 0..5) {
                        Collections.shuffle(masterList)
                    }
                    masterList.forEach { song ->
                        val songReq = song.track
                        PartyQueue.queueSong(botUser!!, party!!, songReq.name, songReq.artists.firstOrNull()?.name ?: "Unknown",
                                songReq.duration, songReq.album.images.firstOrNull()?.url ?: "", songReq.uri)
                        println(queue)
                    }
                }
            }
        }
    }

}

data class ComparableSong(val track: Track) {
    override fun equals(other: Any?): Boolean {
        if (other is ComparableSong) {
            return track.externalIds.externalIds["isrc"] == other.track.externalIds.externalIds["isrc"]
                    || track.name == other.track.name
                    || track.name.split(" ").first().trim() == other.track.name.split(" ").first().trim()
        }
        return super.equals(other)
    }

}

data class BotChannel(val name: String, val description: String, val keywords: List<String> = listOf(), val urlFeed: String? = null)