package uk.bipush.party.endpoint

import spark.Route
import spark.Spark
import uk.bipush.party.model.Account
import uk.bipush.party.model.response
import uk.bipush.party.util.JacksonResponseTransformer


class AccountEndpoint : Endpoint {

    override fun init() {
        Spark.get("/api/v1/account", getAccount, JacksonResponseTransformer())
    }

    val getAccount = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id")
        if (userId != null) {
            val account = Account.finder.byId(userId)
            account?.response(false, false)
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

}