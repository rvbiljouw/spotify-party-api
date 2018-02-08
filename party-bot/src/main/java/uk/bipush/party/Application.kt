@file:JvmName("Application")

package uk.bipush.party

import io.ebean.EbeanServerFactory
import io.ebean.config.ServerConfig
import org.avaje.agentloader.AgentLoader
import org.avaje.datasource.DataSourceConfig
import org.slf4j.LoggerFactory
import uk.bipush.party.bot.BotMother
import uk.bipush.party.bot.spotify.SpotifyBotMother
import uk.bipush.party.bot.youtube.YoutubeBotMother
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author rvbiljouw
 */
private fun bootstrapEbean() {
    val config = ServerConfig()
    config.name = "default"
    config.packages = Arrays.asList(
            "uk.bipush.party.model"
    )
    config.isDefaultServer = true
    config.isUpdateChangesOnly = true

    val datasourceConfig = DataSourceConfig().apply {
        this.driver = "com.mysql.jdbc.Driver"
        this.url = System.getenv("AWSUMIO_JDBC_STRING")
                .replace("--NODE_HOSTNAME--", System.getenv("NODE_HOSTNAME") ?: "")
        this.username = System.getenv("AWSUMIO_JDBC_USERNAME")
        this.password = System.getenv("AWSUMIO_JDBC_PASSWORD")
    }
    config.dataSourceConfig = datasourceConfig
    config.isDdlGenerate = false
    config.isDdlRun = false
    config.isDefaultServer = true

    try {
        EbeanServerFactory.create(config)
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}



val logger = LoggerFactory.getLogger("Application")

fun main(args: Array<String>) {
    if (!AgentLoader.loadAgentFromClasspath("ebean-agent", "debug=1;packages=uk.bipush.party.model.**")) {
        System.err.println("Couldn't load Ebean Agent!")
    }

    bootstrapEbean()

    val poolSize = Runtime.getRuntime().availableProcessors()

    logger.info("Starting up with ${poolSize} threads")

    val executorService = Executors.newScheduledThreadPool(poolSize)

    val botTypes = System.getenv("BOT_TYPES").split(",")

    val mothers = listOf(
            if (botTypes.contains("SPOTIFY")) SpotifyBotMother() else null,
            if (botTypes.contains("YOUTUBE")) YoutubeBotMother() else null
    ).filterNotNull()

    mothers.forEach { mother ->
        val bots = mother.createBots()

        bots.forEach { bot ->
            executorService.scheduleAtFixedRate(bot, 0, 10L, TimeUnit.MINUTES)
        }
    }

    logger.info("Started up!")

    val lock = Object()
    synchronized(lock, {
        lock.wait()
    })
}