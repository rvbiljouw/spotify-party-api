package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import spark.Route
import spark.Spark
import uk.bipush.party.endpoint.net.PartyHandler
import uk.bipush.party.model.Account
import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyQueueEntry
import uk.bipush.party.model.response
import uk.bipush.party.util.DBUtils
import uk.bipush.party.util.JacksonResponseTransformer

class PartyEndpoint : Endpoint {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    override fun init() {
        Spark.put("/api/v1/party", joinParty, JacksonResponseTransformer())
        Spark.delete("/api/v1/party", leaveParty, JacksonResponseTransformer())
        Spark.post("/api/v1/party", createParty, JacksonResponseTransformer())
    }

    val joinParty = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)
        if (account != null) {
            val request: JoinPartyRequest = mapper.readValue(req.body())

            val party = Party.finder.byId(request.id)
            if (party != null) {
                DBUtils.transactional({
                    party.members.add(account)
                    account.activeParty = party

                    party.update()
                    account.update()
                })

                PartyHandler.sendPartyUpdate(party, party.members)

                party.response(false)
            } else {
                res.status(404)
                mapOf("error" to "Party not found.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val leaveParty = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)
        if (account != null) {
            val request: JoinPartyRequest = mapper.readValue(req.body())

            val party = Party.finder.query()
                    .where()
                    .eq("id", request.id)
                    .eq("members.id", account.id)
                    .findUnique()

            if (party != null) {
                DBUtils.transactional({
                    party.members.remove(account)

                    if (account.activeParty == party) {
                        account.activeParty = null
                    }

                    party.update()
                    account.update()
                })

                PartyHandler.sendPartyUpdate(party, party.members)

                party.response(false)
            } else {
                res.status(404)
                mapOf("error" to "Party not found.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val createParty = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.all().get(0)
        if (account != null) {
            val request: CreatePartyRequest = mapper.readValue(req.body())

            val party = Party().apply {
                this.owner = account
                this.name = request.name
                this.description = request.description
                this.members = mutableSetOf(account)
            }

            DBUtils.transactional({
                party.save()

                account.activeParty = party

                account.update()
            })

            party.response(true)
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }
}

data class CreatePartyRequest(val name: String, val description: String)

data class JoinPartyRequest(val id: Long)