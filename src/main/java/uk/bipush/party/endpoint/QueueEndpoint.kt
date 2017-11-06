package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import spark.Route
import spark.route.HttpMethod
import uk.bipush.http.Endpoint
import uk.bipush.http.auth.Auth
import uk.bipush.party.model.*
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.queue.response

class QueueEndpoint {

    companion object {
        private val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/party/:id/queue")
    val getQueue = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val account = token.account!!

        val partyId: String = req.params(":id")
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val party = if (partyId != "active") Party.finder.byId(partyId.toLong()) else account.activeParty
        if (party != null) {
            PartyQueue.forParty(party, offset, limit).response(false)
        } else {
            res.status(403)
            mapOf("error" to "You have no active party.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/party/:id/history")
    val getHistory = Route { req, res ->
        val partyId: String = req.params(":id")
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        val loginToken: LoginToken = req.attribute("account")
        val account = loginToken.account!!

        val party = if (partyId != "active") Party.finder.byId(partyId.toLong()) else account.activeParty
        if (party != null) {
            val entries = PartyQueueEntry.finder.query()
                    .where()
                    .eq("party.id", party.id)
                    .eq("status", PartyQueueEntryStatus.PLAYED)
                    .setFirstRow(offset)
                    .setMaxRows(limit)
                    .order().desc("playedAt")
                    .findPagedList()

            entries.loadCount()

            res.header("X-Max-Records", entries.totalCount.toString())
            res.header("X-Offset", offset.toString())
            entries.list.map { it.response(false, false) }
        } else {
            res.status(404)
            mapOf("error" to "Party not found.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/party/:id/queue")
    val queueSong = Route { req, res ->
        val partyId = req.params(":id")
        val token: LoginToken = req.attribute("account")
        val account = token.account!!


        val party = if (partyId != "active") Party.finder.byId(partyId.toLong()) else account.activeParty
        if (party != null) {
            val request: QueueSongRequest = mapper.readValue(req.body())

            val entry =
                    PartyQueue.queueSong(account, party, request.title, request.artist,
                            request.duration, request.thumbnail, request.uri)

            entry.response(false)
        } else {
            res.status(404)
            mapOf("error" to "Party not found.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.put, uri = "/api/v1/party/:id/queue")
    val voteSong = Route { req, res ->
        val partyId = req.params(":id")
        val token: LoginToken = req.attribute("account")
        val account = token.account!!

        val party = if (partyId != "active") Party.finder.byId(partyId.toLong()) else account.activeParty
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
            res.status(404)
            mapOf("error" to "Party not found.")
        }

    }
}

data class QueueSongRequest(val artist: String, val title: String, val thumbnail: String, val uri: String, val duration: Int)

data class VoteSongRequest(val id: Long, val up: Boolean)