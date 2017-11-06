package uk.bipush.party.task

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.bipush.party.endpoint.QueueSongRequest
import uk.bipush.party.handler.PartyManager
import uk.bipush.party.model.*
import uk.bipush.party.queue.PartyQueue
import java.util.*
import java.util.stream.IntStream
import kotlin.streams.toList

abstract class PartyBot(val partyType: PartyType) : Runnable {
    val mapper = ObjectMapper().registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val botNames = mutableListOf("Scotty", "Donuts", "David", "Jonas", "Joop99")

    abstract fun getChannels(): List<BotChannel>

    abstract fun getTracks(keyword: String): List<Song>

    override fun run() {
        val random = Random()
        val botName = botNames[random.nextInt(botNames.size - 1)]
        var botUser = Account.finder.query().where().eq("displayName", botName).findUnique()
        if (botUser == null) {
            botUser = Account().apply {
                this.accountType = AccountType.BOT
                this.displayName = botName
            }
            botUser.save()
        }

        getChannels().forEach { channel ->
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
                    this.type = partyType
                }
                party.save()

                PartyManager.managers[party.type]?.register(party)
            }

            val queue = PartyQueue.forParty(party, 0, 25)
            if (queue.entries.size < 10) {
                if (channel.urlFeed != null) {
                    val request = Request.Builder().url(channel.urlFeed).get().build()
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
                    val masterList: MutableList<Song> = mutableListOf()
                    channel.keywords.forEach {
                        val results = getTracks(it)
                        if (results.isNotEmpty()) {
                            val songs = IntStream.range(0, results.size - 1)
                                    .map { random.nextInt(results.size - 1) }
                                    .toList().toHashSet()
                                    .take(5)
                                    .map { results[it] }
                            masterList.addAll(songs)
                        }
                    }

                    for (i in 0..5) {
                        Collections.shuffle(masterList)
                    }

                    masterList.forEach { song ->
                        PartyQueue.queueSong(botUser!!, party!!,
                                song.title,
                                song.artist,
                                song.duration,
                                song.thumbnail,
                                song.url)
                    }
                }
            }
        }
    }

}

data class BotChannel(val name: String, val description: String, val keywords: List<String> = listOf(), val urlFeed: String? = null)

data class Song(val title: String, val artist: String, val duration: Int, val thumbnail: String, val url: String)