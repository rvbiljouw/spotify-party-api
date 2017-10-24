package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import spark.Route
import spark.Spark
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.handler.PartyHandler
import uk.bipush.party.model.*
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.queue.response
import uk.bipush.party.util.DBUtils
import uk.bipush.party.util.JacksonResponseTransformer

class QueueEndpoint : Endpoint {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    override fun init() {
        Spark.get("/api/v1/queue", getQueue, JacksonResponseTransformer())
        Spark.post("/api/v1/queue", queueSong, JacksonResponseTransformer())
        Spark.put("/api/v1/queue", voteSong, JacksonResponseTransformer())
    }

    val getQueue = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val partyId: Long? = req.queryParams("party")?.toLong()
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val account = Account.finder.byId(userId)
        if (account != null) {
            val party = if (partyId != null) Party.finder.byId(partyId) else account.activeParty
            if (party != null) {
                PartyQueue.forParty(party, offset, limit).response(false)
            } else {
                res.status(403)
                mapOf("error" to "You have no active party.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val queueSong = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)
        if (account != null) {
            val party = account.activeParty
            if (party != null) {
                val request: QueueSongRequest = mapper.readValue(req.body())

                val entry =
                        PartyQueue.queueSong(account, party, request.title, request.artist,
                                request.duration, request.thumbnail, request.uri)

                entry.response(false)
            } else {
                res.status(403)
                mapOf("error" to "You have no active party.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val voteSong = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)
        if (account != null) {
            val party = account.activeParty
            if (party != null) {
                val request: VoteSongRequest = mapper.readValue(req.body())
                var entry = PartyQueueEntry.finder.byId(request.id)

                if (entry != null) {
                    if (entry.status != PartyQueueEntryStatus.IN_QUEUE) {
                        res.status(400)
                        mapOf("error" to "Unable to find song in the queue.")
                    } else {
                        entry = PartyQueue.voteSong(account, party, entry, request.up)
                        if (entry != null) {
                            entry.response(false)
                        } else {
                            res.status(409)
                            mapOf("error" to "Sorry, you've already voted on this queue entry.")
                        }
                    }
                } else {
                    res.status(404)
                    mapOf("error" to "Queue entry not found.")
                }
            } else {
                res.status(403)
                mapOf("error" to "You have no active party.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }
}

data class QueueSongRequest(val artist: String, val title: String, val thumbnail: String, val uri: String, val duration: Int)

data class VoteSongRequest(val id: Long, val up: Boolean)