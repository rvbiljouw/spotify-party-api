package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

enum class AccountType {
    REGULAR, STAFF,

    BOT
}

@Entity
class Account : Model() {

    companion object {
        val finder: Finder<Long, Account> = Finder(Account::class.java)

        fun find(userId: Long?, loginToken: String?): Account? {
            if (userId != null && userId > 0) {
                val account = Account.finder.byId(userId)

                account?.loginToken = UUID.randomUUID().toString()
                account?.update()

                return account
            } else if (loginToken?.isNotBlank() == true) {

                return Account.finder.query().where().eq("loginToken", loginToken).findUnique()
            }

            return null
        }
    }

    @Id
    var id: Long = 0
    var spotifyId: String? = null
    var displayName: String? = null
    @Index
    var loginToken: String? = null
    @Index
    var accessToken: String? = null
    var refreshToken: String? = null
    @Index
    var selectedDevice: String? = null
    @ManyToOne
    var activeParty: Party? = null
    @Enumerated(value = EnumType.STRING)
    var accountType: AccountType = AccountType.REGULAR
    @CreatedTimestamp
    var created: Timestamp? = null
    @UpdatedTimestamp
    var updated: Timestamp? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

class AccountResponse {
    var id: Long = 0
    var spotifyId: String? = null
    var displayName: String? = null
    var loginToken: String? = null
    var accessToken: String? = null
    var refreshToken: String? = null
    var selectedDevice: String? = null
    var activeParty: PartyResponse? = null
    var created: Timestamp? = null
    var updated: Timestamp? = null
}

fun Account.response(withTokens: Boolean = false, withChildren: Boolean = false, withLoginToken: Boolean = false): AccountResponse {
    val self = this
    return AccountResponse().apply {
        this.id = self.id
        this.spotifyId = self.spotifyId
        this.displayName = self.displayName
        this.selectedDevice = self.selectedDevice
        this.created = self.created
        this.updated = self.updated

        if (withLoginToken) {
            this.loginToken = self.loginToken
        }

        if (withTokens) {
            this.accessToken = self.accessToken
            this.refreshToken = self.refreshToken
        }
        if (withChildren) {
            this.activeParty = self.activeParty?.response(false, false)
        }
    }
}