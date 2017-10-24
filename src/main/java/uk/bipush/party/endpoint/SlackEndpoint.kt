package uk.bipush.party.endpoint

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import io.ebean.Expr
import spark.Route
import spark.Spark
import uk.bipush.party.model.Account
import uk.bipush.party.model.AccountLink
import uk.bipush.party.model.LinkType
import uk.bipush.party.model.response
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.util.JSONUtil
import uk.bipush.party.util.JacksonResponseTransformer
import uk.bipush.party.util.Slack
import uk.bipush.party.util.Spotify
import java.util.*
import java.util.concurrent.TimeUnit


class SlackEndpoint : Endpoint {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val slackAuthCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build<String, AccountLink>()

    override fun init() {
        Spark.get("/slack/callback", slackCallback, JacksonResponseTransformer())

        Spark.post("/slack/action", slackAction, JacksonResponseTransformer())
    }

    private val slackCallback = Route { req, res ->
        try {
            val userId: Long = req.session().attribute("user_id") ?: 0
            val account = Account.finder.byId(userId)
            if (account != null) {
                val state = req.queryParams("state")
                val code = req.queryParams("code")

                var link = slackAuthCache.getIfPresent(code)
                val error = if (link == null) {
                    val authInfo = Slack.authSlackUser(code)

                    if (authInfo != null) {
                        val externalId = "${authInfo.user?.id}-${authInfo.team?.id}"
                        link = AccountLink.finder.query()
                                .where()
                                .or(Expr.eq("token", authInfo.token),
                                        Expr.eq("externalId", externalId))
                                .findUnique()

                        if (link == null) {
                            link = AccountLink().apply {
                                this.externalId = externalId
                                this.token = token
                                this.account = account
                                this.linkType = LinkType.SLACK
                            }

                            link.save()

                            slackAuthCache.put(code, link)

                            ""
                        } else if (link.account != account) {
                            res.status(409)
                            "That slack account is already linked to another account"
                        } else ""
                    } else {
                        res.status(500)
                        "Unable to get token"
                    }
                } else if (link.account != account) {
                    res.status(409)
                    "That slack account is already linked to another account"
                } else ""

                res.redirect("${Spotify.FRONTEND_HOST}?error=${error}")
            } else {
                res.status(403)
                mapOf("error" to "You're not logged in.")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            val exId = UUID.randomUUID().toString()

            res.status(500)
            mapOf("error" to "An error occurred - $exId")
        }
    }


    private val slackAction = Route { req, res ->
        try {
            println(JSONUtil.formToJSON(req.body()).replace("\\\"", "\""))
            val action: SlackActionRequest = mapper.readValue(JSONUtil.formToJSON(req.body()).replace("\\\"", "\""))
            println(action)
            if (action.callbackId == null || action.name == null || action.value == null) {
                res.status(400)
                mapOf("error" to "Invalid action")
            } else {
                when (action.callbackId) {
                    "song_select" -> {
                        val accountId = action.name?.toLong()

                        val account  = Account.finder.byId(accountId)
                        val songSelect: SongSelect = mapper.readValue(action.value!!)
                        val party = account?.activeParty
                        if (party != null && songSelect.uri != null) {
                            val entry = PartyQueue.queueSong(account, party, songSelect.title ?: "-", songSelect.artist ?: "-",
                                    songSelect.duration ?: -0, songSelect.thumbnail ?: "", songSelect.uri!!)

                            entry.response(false)
                        } else {
                            res.status(400)
                            mapOf("error" to "Invalid party or uri")
                        }
                    }
                }

                res.status(404)
                mapOf("error" to "Unhandled callback id")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            val exId = UUID.randomUUID().toString()

            res.status(500)
            mapOf("error" to "An error occurred - $exId")
        }
    }

}

data class SlackActionRequest(@JsonProperty("callback_id") var callbackId: String?, var name: String?, var value: String?)
data class SongSelect(var title: String?, var artist: String?, var duration: Int?, var thumbnail: String?, var uri: String?)