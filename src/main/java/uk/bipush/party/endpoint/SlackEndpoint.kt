package uk.bipush.party.endpoint

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
import spark.route.HttpMethod
import uk.bipush.http.Endpoint
import uk.bipush.party.model.*
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.slack.Slack
import uk.bipush.party.slack.SlackActionHandlers
import uk.bipush.party.slack.SlackCommandHandlers
import uk.bipush.party.util.*
import java.util.*
import java.util.concurrent.TimeUnit


class SlackEndpoint {

    companion object {
        private val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        private val slackAuthCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5L, TimeUnit.MINUTES)
                .build<String, AccountLink>()
    }


    @field:Endpoint(method = HttpMethod.get, uri = "/slack/callback")
    val slackCallback = Route { req, res ->
        try {
            val token: LoginToken = req.attribute("account")
            val account = token.account

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
                                this.token = token.token
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

                res.redirect("${Spotify.FRONTEND_HOST}?error=$error")
            } else {
                res.status(403)
                res.redirect("${Spotify.FRONTEND_HOST}?error=You're not logged in")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            val exId = UUID.randomUUID().toString()

            res.status(500)
            mapOf("error" to "An error occurred - $exId")
        }
    }

    private val slackCommnad = Route { req, res ->
        val slackCommandRequest: SlackCommandRequest = mapper.readValue(JSONUtil.formToJSON(req.body()))
        val account = AccountLink.finder.query().where().eq("externalId",
                "${slackCommandRequest.userId}-${slackCommandRequest.teamId}")
                .findUnique()?.account

        if (account != null) {
            when (slackCommandRequest.command?.toLowerCase()) {
                "/searchsongs" -> SlackCommandHandlers.handleSearchSongs(account, slackCommandRequest)
                "/upvote" -> SlackCommandHandlers.handleListVote(account, slackCommandRequest, true)
                "/downvote" -> SlackCommandHandlers.handleListVote(account, slackCommandRequest, false)
                "/nowplaying" -> SlackCommandHandlers.handleListNowPlaying(account, slackCommandRequest)
                "/queue" -> SlackCommandHandlers.handleListQueue(account)
                null -> SlackActionResponse("ephemeral", false, "Invalid command")
                else -> SlackActionResponse("ephemeral", false, "Unknown command")
            }
        } else {
            SlackActionResponse("ephemeral", false, "Unable to find your account, please connect it on the website.")
        }
    }

    private val slackAction = Route { req, res ->
        try {
            val wrapper: SlackActionRequestWrapper = mapper.readValue(JSONUtil.formToJSON(req.body()))
            val actions: SlackActionRequest = mapper.readValue(wrapper.payload)
            if (actions.callbackId == null || actions.actions == null || actions.actions!!.isEmpty()) {
                res.status(400)
                SlackActionResponse("ephemeral", false, "I don't know how to handle this")
            } else {
                val action = actions.actions!![0]
                if (action.name == null || action.value == null) {
                    res.status(400)
                    SlackActionResponse("ephemeral", false, "I don't know how to handle this :/")
                } else {
                    when (actions.callbackId) {
                        "song_select" -> SlackActionHandlers.handleSongSelect(action)
                        "song_upvote" -> SlackActionHandlers.handleSongVote(action, true)
                        "song_downvote" -> SlackActionHandlers.handleSongVote(action, false)
                        else -> {
                            res.status(404)
                            SlackActionResponse("ephemeral", false, "I don't know how to handle this :(")
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            val exId = UUID.randomUUID().toString()

            res.status(500)
            mapOf("error" to "An error occurred - $exId")
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackActionRequestWrapper(var payload: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackActionRequest(@JsonProperty("callback_id") var callbackId: String?, var actions: List<SlackAction>?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackAction(var text: String?, var name: String?, var value: String?, var type: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SongSelect(var title: String?, var artist: String?, var duration: Int?, var thumbnail: String?, var uri: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackActionResponse(@JsonProperty("response_type") var responseType: String,
                               @JsonProperty("replace_original") var replaceOriginal: Boolean,
                               var text: String,
                               var attachments: List<SlackAttachment> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackCommandRequest(var token: String?,
                               @JsonProperty("team_id") var teamId: String?,
                               @JsonProperty("team_domain") var teamDomain: String?,
                               @JsonProperty("channel_id") var channelId: String?,
                               @JsonProperty("channel_name") var channelName: String?,
                               @JsonProperty("user_id") var userId: String?,
                               @JsonProperty("user_name") var userName: String?,
                               var command: String?,
                               var text: String?,
                               @JsonProperty("response_url") var responseUrl: String?,
                               @JsonProperty("trigger_id") var triggerId: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackAttachment(var text: String?, var fallback: String?,
                           @JsonProperty("callback_id") var callbackId: String?,
                           var color: String?, var actions: List<SlackAction>)
