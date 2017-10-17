@file:JvmName("Application")

package uk.bipush.party

import org.avaje.agentloader.AgentLoader
import spark.Spark
import uk.bipush.party.endpoint.*
import uk.bipush.party.endpoint.net.PartyHandler
import uk.bipush.party.util.Spotify
import uk.bipush.party.util.SpotifyFilter

/**
 * @author rvbiljouw
 */
fun main(args: Array<String>) {
    if (!AgentLoader.loadAgentFromClasspath("ebean-agent", "debug=1;packages=uk.bipush.party.model.**")) {
        System.err.println("Couldn't load Ebean Agent!")
    }

val endpoints = arrayOf(LoginEndpoint(), AccountEndpoint(), DevicesEndpoint(),
        MusicEndpoint(), PartyEndpoint(), QueueEndpoint())
    Spark.exception(Exception::class.java, { t, request, response ->
        t.printStackTrace()
    })
    Spark.port(8080)

    Spark.webSocket("/api/v1/partySocket", PartyHandler::class.java)

    Spark.init()

    endpoints.forEach { it.init() }
}