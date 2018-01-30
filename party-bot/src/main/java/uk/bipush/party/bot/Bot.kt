package uk.bipush.party.bot

import org.slf4j.LoggerFactory
import uk.bipush.party.api.PartyApi
import uk.bipush.party.api.QueueSongRequest
import uk.bipush.party.model.*
import java.util.*

abstract class Bot: Runnable {

    val logger = LoggerFactory.getLogger(javaClass)

    abstract fun getBotAccount(): Account

    abstract fun getBotParty(): Party

    abstract fun getNextSongs(): Set<QueueSongRequest>

    override fun run() {
        val party = getBotParty()

        logger.info("[${party.name}] Bot loop running")

        val token = getToken()

        val next = getNextSongs()

        logger.info("[${party.name}] Queueing ${next.size} songs")
        next.forEach { req ->
            val result = PartyApi.queueSong(token.token!!, party, req)

            if (!result) {
                logger.warn("[${party.name}] Failed to queue song")
            }
        }
    }

    fun getToken(): LoginToken {
        val account = getBotAccount()
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