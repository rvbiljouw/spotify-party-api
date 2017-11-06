package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.client.auth.oauth2.TokenResponse
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
import uk.bipush.party.model.Account
import uk.bipush.party.model.LoginToken
import uk.bipush.party.model.LoginTokenResponse
import uk.bipush.party.model.LoginTokenStatus
import java.util.*

/**
 * @author rvbiljouw
 */
class LoginEndpoint {

    companion object {
        val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/login")
    val validate = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        LoginTokenResponse(token)
    }

    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/login")
    val login = Route { req, res ->
        val loginRequest: LoginRequest = mapper.readValue(req.body())
        val errors = loginRequest.validate()
        if (errors.isEmpty()) {
            val account = Account.finder.query().where()
                    .eq("email", loginRequest.email)
                    .findUnique()
            if (account != null && BCrypt.checkpw(loginRequest.password, account.password)) {
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
                req.session(true).attribute("token", account.loginToken?.token)
                LoginTokenResponse(account.loginToken!!)
            } else {
                res.error(Errors.forbidden, "Invalid login credentials")
            }
        } else {
            res.error(Errors.badRequest, errors.map { it.response() })
        }
    }

}

data class LoginRequest(@field:NotEmpty @field:Email val email: String?, @field:NotEmpty val password: String?) : ValidatedRequest()