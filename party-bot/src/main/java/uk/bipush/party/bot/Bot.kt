package uk.bipush.party.bot

import org.slf4j.LoggerFactory
import uk.bipush.party.api.CreatePartyRequest
import uk.bipush.party.api.PartyApi
import uk.bipush.party.api.QueueSongRequest
import uk.bipush.party.model.*
import java.util.*

abstract class Bot(val botMother: BotMother<*>): Runnable {

    val logger = LoggerFactory.getLogger(javaClass)

    abstract fun getNextSongs(): Set<QueueSongRequest>

    abstract fun getPlaylistId(): String

    abstract fun getPlaylistName(): String

    abstract fun getPlaylistDescription(): String

    abstract fun getPlaylistOwnerId(): String

    fun getCreateBotParty(): Party {
        var playlistParty = PlaylistParty.finder.query().where()
                .eq("party.type", botMother.type)
                .eq("playlistId", getPlaylistId())
                .eq("playlistOwnerId", getPlaylistOwnerId())
                .findOne()

        val token = getToken()

        if (playlistParty == null) {
            val party = PartyApi.createParty(
                    token.token!!,
                    CreatePartyRequest(
                            botMother.type,
                            PartyAccess.PUBLIC,
                            getPlaylistName(),
                            getPlaylistDescription()
                    )
            )

            playlistParty = PlaylistParty().apply {
                this.party = party
                this.playlistId = getPlaylistId()
                this.playlistOwnerId = getPlaylistOwnerId()
            }

            playlistParty.save()
        }

        return playlistParty.party!!
    }

    fun shouldQueue(party: Party, songId: String): Boolean {
        val entries = PartyQueueEntry.finder.query().where()
                .eq("party.id", party.id)
                .eq("songId", songId)
                .`in`("status", listOf(PartyQueueEntryStatus.PLAYING, PartyQueueEntryStatus.IN_QUEUE))
                .setMaxRows(1)
                .findCount()

        return entries == 0
    }

    override fun run() {
        try {
            val party = getCreateBotParty()

            logger.info("[${party.name}] Bot loop running")

            val token = getToken()

            val next = getNextSongs()

            logger.info("[${party.name}] Queueing ${next.size} songs")
            val count = next.mapNotNull { req ->
                val result = PartyApi.queueSong(token.token!!, party, req)

                if (!result) {
                    logger.warn("[${party.name}] Failed to queue song")
                    null
                } else {
                    Any()
                }
            }.size

            logger.info("[${party.name}] Queued ${next.size} songs")
        } catch (e: Error) {
            logger.error("Error running bot loop", e)
        }
    }

    fun getToken(): LoginToken {
        val account = botMother.getCreateBotAccount()
        var token = account.loginToken

        if (token == null) {
            token = LoginToken().apply {
                this.account = account
                this.status = LoginTokenStatus.ACTIVE
                this.token = UUID.randomUUID().toString()
            }

            token.save()
        }

        return token
    }
}