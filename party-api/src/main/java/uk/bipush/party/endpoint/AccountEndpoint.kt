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
import com.google.common.cache.CacheBuilder
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.NotEmpty
import org.mindrot.jbcrypt.BCrypt
import spark.Route
import spark.Spark
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
import uk.bipush.party.util.JacksonResponseTransformer
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
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
    @field:Endpoint(uri = "/api/v1/account/picture", method = HttpMethod.put)
    val uploadProfilePicture = Route { req, res ->
        val multipartConfigElement = MultipartConfigElement("/tmp/")
        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)
        val token: LoginToken = req.attribute("account")
        val account = token.account!!

        val filePart = req.raw().getPart("file")
        var fileName = filePart.submittedFileName

        val file = File("/tmp/${System.currentTimeMillis()}.tmp")
        filePart.write(file.name)
        if (file.exists()) {
            val hash = Files.hash(file, Hashing.adler32()).toString()
            fileName = getBlobFileName(hash, fileName)

            val blob = storage.create(BlobInfo
                    .newBuilder("awsumio-storage", fileName)
                    .setAcl(listOf(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                    .setMetadata(mapOf("adlerHash" to hash)).build(),
                    file.inputStream())
            account.displayPicture = blob.mediaLink
            account.save()

            account.response()
        } else {
            res.status(500)
        }
    }


    private fun getBlobFileName(hash: String, fileName: String): String {
        return "$hash-${System.currentTimeMillis()}-$fileName"
    }


}

data class CreateAccountRequest(@field:NotEmpty @field:Email val email: String?,
                                @field:NotEmpty @field:Length(min = 8) val password: String?,
                                @field:NotEmpty val displayName: String?) : ValidatedRequest()

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateAccountRequest(val email: String?,
                                val newPassword: String?,
                                val displayName: String?) : ValidatedRequest()