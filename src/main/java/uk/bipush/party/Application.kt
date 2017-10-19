@file:JvmName("Application")

package uk.bipush.party

import com.wrapper.spotify.Api
import org.avaje.agentloader.AgentLoader
import spark.Filter
import spark.Route
import spark.Spark
import uk.bipush.party.endpoint.*
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.handler.PartyHandler
import uk.bipush.party.model.Account
import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyStatus
import uk.bipush.party.task.OfflineDeviceUpdater
import uk.bipush.party.util.Spotify
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author rvbiljouw
 */

private val optionsHandler = Route { request, response ->
    val accessControlRequestHeaders = request.headers("Access-Control-Request-Headers")
    if (accessControlRequestHeaders != null) {
        response.header("Access-Control-Allow-Headers", accessControlRequestHeaders)
    }

    val accessControlRequestMethod = request.headers("Access-Control-Request-Method")
    if (accessControlRequestMethod != null) {
        response.header("Access-Control-Allow-Methods", accessControlRequestMethod)
    }

    "OK"
}

fun main(args: Array<String>) {
    if (!AgentLoader.loadAgentFromClasspath("ebean-agent", "debug=1;packages=uk.bipush.party.model.**")) {
        System.err.println("Couldn't load Ebean Agent!")
    }

    val partyHandler = PartyHandler()
    Party.finder.query().where().eq("status", PartyStatus.ONLINE)
            .findIds<Long>().forEach { partyHandler.addParty(it) }

    val endpoints = arrayOf(LoginEndpoint(), AccountEndpoint(), DevicesEndpoint(),
            MusicEndpoint(), PartyEndpoint(partyHandler), QueueEndpoint())
    Spark.exception(Exception::class.java, { t, request, response ->
        t.printStackTrace()
    })
    Spark.port(8080)

    Spark.webSocket("/api/v1/partySocket", PartyWebSocket::class.java)

    Spark.init()

    Spark.before(Filter { request, response ->
        response.header("Access-Control-Allow-Origin", request.headers("Origin"));
        response.header("Access-Control-Request-Method", "GET,PUT,POST,DELETE");
        response.header("Access-Control-Allow-Headers",
                "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,X-Offset,X-Max-Records");
        response.header("Access-Control-Expose-Headers",
                "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,X-Offset,X-Max-Records");
        response.header("Access-Control-Allow-Credentials", "true");
        // Note: this may or may not be necessary in your particular
        // application
        response.type("application/json");
    })

    Spark.options("/*", optionsHandler)

    endpoints.forEach { it.init() }

    val executorService = Executors.newScheduledThreadPool(2)
    executorService.scheduleAtFixedRate(partyHandler, 0, 2, TimeUnit.SECONDS)
    executorService.scheduleAtFixedRate(OfflineDeviceUpdater(), 0, 2, TimeUnit.SECONDS)

    executorService.scheduleAtFixedRate(Runnable {
        val api = Api.builder()
                .clientId(Spotify.CLIENT_ID)
                .clientSecret(Spotify.CLIENT_SECRET)
                .redirectURI("http://localhost:8080/callback")
                .build()

        val tokens = Account.finder.query()
                .setFirstRow(0)
                .setMaxRows(25)
                .findPagedList()
        tokens.loadCount()

        val count = tokens.totalCount
        var offset = 0
        while (offset < count) {
            val results = Account.finder.query()
                    .setFirstRow(offset)
                    .setMaxRows(25)
                    .findPagedList()
            results.list.forEach({ acc ->
                api.setAccessToken(acc.accessToken)
                api.setRefreshToken(acc.refreshToken)
                val newToken = api.refreshAccessToken()
                        .refreshToken(acc.refreshToken)
                        .build().get()
                acc.accessToken = newToken.accessToken
                acc.update()
                println("Updated ${acc.displayName}")
            })
            offset += results.list.size
        }
    }, 0, 5L, TimeUnit.MINUTES)
}