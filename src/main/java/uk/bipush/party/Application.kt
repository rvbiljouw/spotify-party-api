@file:JvmName("Application")

package uk.bipush.party

import org.avaje.agentloader.AgentLoader
import spark.ExceptionHandler
import spark.Spark
import uk.bipush.party.endpoint.AccountEndpoint
import uk.bipush.party.endpoint.LoginEndpoint

/**
 * @author rvbiljouw
 */
fun main(args: Array<String>) {
    if (!AgentLoader.loadAgentFromClasspath("ebean-agent", "debug=1;packages=uk.airglide.**")) {
        System.err.println("Couldn't load Ebean Agent!")
    }

    val endpoints = arrayOf(LoginEndpoint(), AccountEndpoint())
    Spark.exception(Exception::class.java, { t, request, response ->
        t.printStackTrace()
    })
    Spark.port(8080)
    Spark.init()

    endpoints.forEach { it.init() }
}