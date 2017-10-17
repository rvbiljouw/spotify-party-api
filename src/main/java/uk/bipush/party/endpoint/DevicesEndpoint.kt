package uk.bipush.party.endpoint

import spark.Route
import spark.Spark
import uk.bipush.party.model.Account
import uk.bipush.party.util.JacksonResponseTransformer
import uk.bipush.party.util.Spotify

class DevicesEndpoint : Endpoint {

    override fun init() {
        Spark.get("/api/v1/devices", getDevices, JacksonResponseTransformer())
        Spark.get("/api/v1/devices/:id/select", selectDevice, JacksonResponseTransformer())
    }

    val getDevices = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id")
        if (userId != null) {
            val account = Account.finder.byId(userId)
            if (account != null) {
                Spotify.getDevices(account.accessToken!!)
            } else {
                res.status(403)
                mapOf("error" to "You're not logged in.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val selectDevice = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id")
        if (userId != null) {
            val account = Account.finder.byId(userId)
            if (account != null) {
                val deviceId = req.params(":id")
                account.selectedDevice = deviceId
                account.save()
                mapOf("success" to true)
            } else {
                res.status(403)
                mapOf("error" to "You're not logged in.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

}