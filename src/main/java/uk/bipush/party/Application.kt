@file:JvmName("Application")

package uk.bipush.party

import org.avaje.agentloader.AgentLoader
import spark.Spark
import uk.bipush.party.endpoint.AccountEndpoint
import uk.bipush.party.endpoint.DevicesEndpoint
import uk.bipush.party.endpoint.LoginEndpoint

/**
 * @author rvbiljouw
 */
fun main(args: Array<String>) {
    if (!AgentLoader.loadAgentFromClasspath("ebean-agent", "debug=1;packages=uk.bipush.party.model.**")) {
        System.err.println("Couldn't load Ebean Agent!")
    }

    val endpoints = arrayOf(LoginEndpoint(), AccountEndpoint(), DevicesEndpoint())
    Spark.exception(Exception::class.java, { t, request, response ->
        t.printStackTrace()
    })
    Spark.port(8080)
    Spark.init()

    endpoints.forEach { it.init() }
}