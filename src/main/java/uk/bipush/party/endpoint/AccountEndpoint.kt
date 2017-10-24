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

    private val slackAuthCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build<String, AccountLink>()

    override fun init() {
        Spark.get("/api/v1/account", getAccount, JacksonResponseTransformer())
    }

    val getAccount = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id")
        val externalId: String? = req.queryParams("externalId")
        if (userId != null) {
            val account = Account.finder.byId(userId)

            if (account != null && account.loginToken == null) {
                account.loginToken = UUID.randomUUID().toString()
                account.update()
            }

            account?.response(false, false, true)
        } else if (externalId != null) {
            val accountLink = AccountLink.finder.query().where().eq("externalId", externalId).findUnique()

            if (accountLink?.account != null) {
                if (accountLink.account!!.loginToken == null) {
                    accountLink.account!!.loginToken = UUID.randomUUID().toString()
                    accountLink.account!!.update()
                }

                accountLink.account!!.response(false, true, true)
            } else {
                res.status(404)
                mapOf("error" to "No account found.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }


}