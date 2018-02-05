package uk.bipush.party.endpoint

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.storage.Acl
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.common.hash.Hashing
import com.google.common.io.Files
import io.ebean.Ebean
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.NotEmpty
import org.mindrot.jbcrypt.BCrypt
import spark.Route
import spark.route.HttpMethod
import uk.bipush.http.Endpoint
import uk.bipush.http.auth.Auth
import uk.bipush.http.response.ErrorResponse
import uk.bipush.http.response.Errors
import uk.bipush.http.response.error
import uk.bipush.http.response.response
import uk.bipush.http.util.ValidatedRequest
import uk.bipush.http.util.validate
import uk.bipush.party.model.*
import java.io.File
import java.util.*
import javax.servlet.MultipartConfigElement


class AccountEndpoint {
    companion object {
        private val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        private val storage: Storage = StorageOptions.getDefaultInstance().service
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/account")
    val getAccount = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        if (token.account != null) {
            token.account?.response(withChildren = false, withLoginToken = true)
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/account/:id")
    val getAccountById = Route { req, res ->
        val token: LoginToken = req.attribute("account")

        val idParam: Long = req.params(":id").toLong()
        val account = Account.finder.byId(idParam)
        if (account != null) {
            account.response(withChildren = true, withLoginToken = false, onlyPublic = account.id != token.account?.id)
        } else {
            res.status(404)
            mapOf("error" to "Account not found or no access to view.")
        }
    }

    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/account")
    val createAccount = Route { req, res ->
        val createRequest: CreateAccountRequest = mapper.readValue(req.body())
        val errors = createRequest.validate()
        if (errors.isEmpty()) {
            if (Account.finder.query().where().eq("email", createRequest.email).findUnique() == null) {
                val account = Account().apply {
                    this.email = createRequest.email
                    this.password = BCrypt.hashpw(createRequest.password, BCrypt.gensalt())
                    this.displayName = createRequest.displayName
                    this.achievements = listOf(Achievement.getJoinedAchievement())
                }
                account.save()

                if (account.loginToken == null) {
                    account.loginToken = LoginToken().apply {
                        this.account = account
                        this.userAgent = req.userAgent()
                        this.ipAddress = req.ip()
                        this.status = LoginTokenStatus.ACTIVE
                        this.token = UUID.randomUUID().toString()
                    }
                    account.loginToken?.save()
                    account.save()
                }

                req.session(true).attribute("token", account.loginToken!!.token)

                account.response(false, true)
            } else {
                res.error(Errors.conflict, "E-mail address is already registered.")
            }
        } else {
            res.error(Errors.badRequest, errors.map { it.response() })
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.put, uri = "/api/v1/account")
    val updateAccount = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val updateRequest: UpdateAccountRequest = mapper.readValue(req.body())
        val errors = updateRequest.validate()
        if (errors.isEmpty()) {
            val accountWithEmail = Account.finder.query().where()
                    .eq("email", updateRequest.email ?: token.account?.email)
                    .findUnique()

            if (accountWithEmail != null && accountWithEmail.id != token.account?.id) {
                res.error(Errors.conflict, ErrorResponse("That email is alaready in use"))
            } else {
                val account = token.account!!

                account.email = updateRequest.email ?: account.email
                account.displayName = updateRequest.displayName ?: account.displayName
                account.password = if (updateRequest.newPassword?.isNotBlank() == true)
                    BCrypt.hashpw(updateRequest.newPassword, BCrypt.gensalt())
                else
                    account.password

                account.update()

                account.response(false, true)
            }
        } else {
            res.error(Errors.badRequest, errors.map { it.response() })
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.put, uri = "/api/v1/account/picture")
    val uploadProfilePicture = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val account = token.account!!

        val fileName = "${System.currentTimeMillis()}-${account.id}.png"

        val blob = storage.create(BlobInfo
                .newBuilder("awsumio-storage", fileName)
                .setAcl(listOf(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                .build(),
                Base64.getDecoder().decode(req.body().split("base64,").last()))

        account.displayPicture = blob.mediaLink
        account.save()

        account.response()
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.put, uri = "/api/v1/account/:id/follow")
    val follow = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val account = token.account!!

        val toFollow = Account.finder.byId(req.params(":id")?.toLong() ?: 0)

        if (toFollow == null) {
            res.error(Errors.notFound)
        } else {

            if (Follower.finder.query().where()
                            .eq("following.id", toFollow.id)
                            .eq("follower.id", account.id)
                            .findCount() > 0) {
                res.error(Errors.conflict)
            } else {
                val follower = Follower().apply {
                    this.following = toFollow
                    this.follower = account
                }

                follower.save()

                follower.response()
            }
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.delete, uri = "/api/v1/account/:id/follow")
    val unfollow = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val account = token.account!!

        val toFollow = Account.finder.byId(req.params(":id")?.toLong() ?: 0)

        if (toFollow == null) {
            res.error(Errors.notFound)
        } else {
            val follower = Follower.finder.query().where()
                            .eq("following.id", toFollow.id)
                            .eq("follower.id", account.id).findOne()
            if (follower != null) {
                follower.delete()

                follower.response()
            } else {
                res.error(Errors.notFound)
            }
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/account/:id/following")
    val isFollowing = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val account = token.account!!

        val toFollow = Account.finder.byId(req.params(":id")?.toLong() ?: 0)

        if (toFollow == null) {
            res.error(Errors.notFound)
        } else {

            if (Follower.finder.query().where()
                            .eq("following.id", toFollow.id)
                            .eq("follower.id", account.id)
                            .findCount() > 0) {
                mapOf("following" to true)
            } else {
                mapOf("following" to false)
            }
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/account/:id/followers")
    val getFollowers = Route { req, res ->
        val account = Account.finder.byId(req.params(":id")?.toLong() ?: 0)

        val limit = req.queryParams("limit")?.toInt()
        val offset = req.queryParams("offset")?.toInt()

        if (account == null) {
            res.error(Errors.notFound)
        } else {
            val results = Follower.finder.query()
                    .where().eq("following.id", account.id)
                    .setFirstRow(offset ?: 0)
                    .setMaxRows(limit ?: 25)
                    .findPagedList()

            results.loadCount()

            res.header("X-Max-Records", results.totalCount.toString())
            res.header("X-Offset", (offset ?: 0).toString())
            results.list.mapNotNull { x -> x.follower?.response(false ,false) }
        }
    }

}

data class CreateAccountRequest(@field:NotEmpty @field:Email val email: String?,
                                @field:NotEmpty @field:Length(min = 8) val password: String?,
                                @field:NotEmpty val displayName: String?) : ValidatedRequest()

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateAccountRequest(val email: String?,
                                val newPassword: String?,
                                val displayName: String?) : ValidatedRequest()