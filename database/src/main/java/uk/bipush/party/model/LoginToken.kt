package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*

enum class LoginTokenStatus {
    ACTIVE, EXPIRED
}

@Entity
class LoginToken : Model() {

    companion object {
        val finder = Finder<Long, LoginToken>(LoginToken::class.java)
    }

    @Id
    var id: Long? = null
    @ManyToOne
    var account: Account? = null
    @Enumerated(EnumType.STRING)
    var status: LoginTokenStatus? = null
    var token: String? = null
    var ipAddress: String? = null
    var userAgent: String? = null
    var lastSeen: DateTime? = null
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}

class LoginTokenResponse(token: LoginToken) {
    var id: Long? = token.id
    var account: AccountResponse? = token.account?.response()
    var status: LoginTokenStatus? = token.status
    var token: String? = token.token
    var created: DateTime? = null
    var updated: DateTime? = null
}

fun LoginToken.response(): LoginTokenResponse {
    return LoginTokenResponse(this)
}