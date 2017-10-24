package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.cache.CacheBuilder
import spark.Route
import spark.Spark
import uk.bipush.party.model.Account
import uk.bipush.party.model.AccountLink
import uk.bipush.party.model.response
import uk.bipush.party.util.JacksonResponseTransformer
import java.util.*
import java.util.concurrent.TimeUnit


class AccountEndpoint : Endpoint {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    override fun init() {
        Spark.get("/api/v1/account", getAccount, JacksonResponseTransformer())
    }

    val getAccount = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id")
        val loginToken: String? = req.queryParams("loginToken")

        val account = Account.find(userId, loginToken)

        if (account != null) {
            account.response(false, false, true)
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }


}