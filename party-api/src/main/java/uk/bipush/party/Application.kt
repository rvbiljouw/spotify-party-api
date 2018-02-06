@file:JvmName("Application")

package uk.bipush.party

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.name.Names
import emoji4j.EmojiUtils
import io.ebean.EbeanServerFactory
import io.ebean.config.ServerConfig
import org.avaje.agentloader.AgentLoader
import org.avaje.datasource.DataSourceConfig
import uk.bipush.http.BasicWebModule
import uk.bipush.http.RouterFactory
import uk.bipush.http.auth.model.AuthenticableRepository
import uk.bipush.http.builtin.filter.TenantContextFilter
import uk.bipush.party.endpoint.*
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.handler.PartyManager
import uk.bipush.party.task.PartyUpdater
import uk.bipush.party.task.TokenRefresher
import uk.bipush.party.util.AccountRepository
import uk.bipush.party.util.ExpireableRunnable
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
    config.isDdlGenerate = true
    config.isDdlRun = false
    config.isDefaultServer = true

    try {
        EbeanServerFactory.create(config)
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

fun main(args: Array<String>) {
    if (!AgentLoader.loadAgentFromClasspath("ebean-agent", "debug=1;packages=uk.bipush.party.model.**")) {
        System.err.println("Couldn't load Ebean Agent!")
    }

    bootstrapEbean()

    TenantContextFilter.USE_REFERRER = true

    val endpoints = arrayOf(
            PartyWebSocket::class.java,
            LoginEndpoint::class.java,
            AccountEndpoint::class.java,
            PartyEndpoint::class.java,
            QueueEndpoint::class.java,
            SlackEndpoint::class.java,
            SpotifyEndpoint::class.java,
            NotificationEndpoint::class.java,
            YouTubeEndpoint::class.java,
            FavouriteEndpoint::class.java)
    val injector = Guice.createInjector(object : AbstractModule() {
        override fun configure() {
            install(BasicWebModule())
            bind(AuthenticableRepository::class.java).annotatedWith(Names.named("default")).toInstance(AccountRepository())
        }
    })

    val routerFactory = injector.getInstance(RouterFactory::class.java)
    val router = routerFactory.create(endpoints, 8080)

    router.init(true)

    val executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())

    executorService.scheduleAtFixedRate(ExpireableRunnable("TokenRefresher", TokenRefresher()), 0, 5L, TimeUnit.MINUTES)
    executorService.scheduleAtFixedRate(ExpireableRunnable("PartyUpdater", PartyUpdater()), 0, 1L, TimeUnit.MINUTES)

    PartyManager.managers.forEach { t, u -> Thread(u).start() }
}