package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import java.sql.Timestamp
import javax.persistence.*

enum class AccountType {
    REGULAR, STAFF,

    BOT
}

enum class SubscriptionType {
    FREE, PREMIUM
}

@Entity
class Account : Model() {

    companion object {
        val finder: Finder<Long, Account> = Finder(Account::class.java)

        fun find(userId: Long?, loginToken: String?): Account? {
            if (userId != null && userId > 0) {
                val account = Account.finder.byId(userId)
// TODO: wtf is this
//                account?.loginToken = UUID.randomUUID().toString()
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
    @Enumerated(value = EnumType.STRING)
    var accountType: AccountType = AccountType.REGULAR
    @ManyToOne
    var subscription: Subscription? = null
    @ManyToMany
    var achievements: List<Achievement>? = null
    @Index
    var email: String? = null
    var password: String? = null
    var displayPicture: String? = null
    var displayName: String? = null
    var hasSpotify: Boolean = false
    @OneToOne
    var spotify: SpotifyAccount? = null
    @ManyToOne
    var loginToken: LoginToken? = null
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
    var accountType: AccountType? = null
    var subscription: Subscription? = null
    var achievements: List<Achievement>? = null
    var followers: List<AccountResponse>? = null
    var email: String? = null
    var displayName: String? = null
    var displayPicture: String? = null
    var loginToken: LoginTokenResponse? = null
    var hasSpotify: Boolean = false
    var spotify: SpotifyAccountResponse? = null
    var created: Timestamp? = null
    var updated: Timestamp? = null
}

fun Account.response(withChildren: Boolean = false, withLoginToken: Boolean = false, onlyPublic: Boolean = false): AccountResponse {
    val self = this
    return AccountResponse().apply {
        this.id = self.id
        if (!onlyPublic) {
            this.email = self.email
            this.subscription = self.subscription
        }

        this.accountType = self.accountType
        this.displayName = self.displayName
        this.displayPicture = self.displayPicture
        this.created = self.created
        this.updated = self.updated

        this.hasSpotify = self.hasSpotify

        if (withLoginToken) {
            this.loginToken = self.loginToken?.response(false)
        }

        if (withChildren) {
            this.achievements = self.achievements
            this.spotify = self.spotify?.response(false, false)
        }
    }
}