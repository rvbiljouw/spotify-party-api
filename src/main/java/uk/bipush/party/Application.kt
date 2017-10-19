@file:JvmName("Application")

package uk.bipush.party

import org.avaje.agentloader.AgentLoader
import spark.Spark
import uk.bipush.party.endpoint.*
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.handler.PartyHandler
import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyStatus
import uk.bipush.party.task.OfflineDeviceUpdater
import uk.bipush.party.util.Spotify
import uk.bipush.party.util.SpotifyFilter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author rvbiljouw
 */
fun main(args: Array<String>) {
    if (!AgentLoader.loadAgentFromClasspath("ebean-agent", "debug=1;packages=uk.bipush.party.model.**")) {
        System.err.println("Couldn't load Ebean Agent!")
    }

    val partyHandler = PartyHandler()
    Party.finder.query().where().eq("status", PartyStatus.ONLINE)
            .findIds<Long>().forEach{ partyHandler.addParty(it) }

    val endpoints = arrayOf(LoginEndpoint(), AccountEndpoint(), DevicesEndpoint(),
            MusicEndpoint(), PartyEndpoint(partyHandler), QueueEndpoint())
    Spark.exception(Exception::class.java, { t, request, response ->
        t.printStackTrace()
    })
    Spark.port(8080)

    Spark.webSocket("/api/v1/partySocket", PartyWebSocket::class.java)

    Spark.init()

    endpoints.forEach { it.init() }

    val executorService = Executors.newScheduledThreadPool(2)
    executorService.scheduleAtFixedRate(partyHandler, 0, 2, TimeUnit.SECONDS)
    executorService.scheduleAtFixedRate(OfflineDeviceUpdater(), 0, 2, TimeUnit.SECONDS)
}