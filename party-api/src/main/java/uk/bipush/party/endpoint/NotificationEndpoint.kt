package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.client.auth.oauth2.TokenResponse
import io.ebean.Ebean
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.NotEmpty
import org.mindrot.jbcrypt.BCrypt
import spark.Route
import spark.route.HttpMethod
import uk.bipush.http.Endpoint
import uk.bipush.http.auth.Auth
import uk.bipush.http.response.Errors
import uk.bipush.http.response.error
import uk.bipush.http.response.response
import uk.bipush.http.util.ValidatedRequest
import uk.bipush.http.util.validate
import uk.bipush.party.model.*
import java.util.*

class NotificationEndpoint {

    companion object {
        val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/notifications/unread")
    val getUnread = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val limit = req.queryParams("limit")?.toInt() ?: 25
        val offset = req.queryParams("offset")?.toInt() ?: 0

        if (token.account != null) {
            val results = Notification.finder.query().where()
                    .eq("account.id", token.account!!.id)
                    .eq("read", false)
                    .setFirstRow(offset)
                    .setMaxRows(limit)
                    .findPagedList()

            results.loadCount()

            res.header("X-Max-Records", results.totalCount.toString())
            res.header("X-Offset", (offset).toString())

            results.list.map { it.response(true) }
        } else {
            res.error(Errors.forbidden)
        }
    }

    @field:Endpoint(method = HttpMethod.put, uri = "/api/v1/notifications/mark")
    val markRead = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val notificationIds: List<Long> = mapper.readValue(req.body())

        if (token.account != null) {
            val updated = Ebean.getDefaultServer().createSqlUpdate("UPDATE notification " +
                    "SET read = 1 WHERE account_id = :accountId AND id IN (:ids)")
                    .setParameter(":accountId", token.account!!.id)
                    .setParameter(":ids", notificationIds)
                    .execute()

            mapOf("numberMarked" to updated)
        } else {
            res.error(Errors.forbidden)
        }
    }

}