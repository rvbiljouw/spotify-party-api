package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import java.sql.Timestamp
import javax.persistence.*

enum class LinkType {
    SLACK;
}
@Entity
class AccountLink : Model() {

    companion object {
        val finder: Finder<Long, AccountLink> = Finder(AccountLink::class.java)
    }

    @Id
    var id: Long = 0
    @Column(unique = true)
    var externalId: String? = null
    @Column(unique = true)
    var token: String? = null
    @ManyToOne
    var account: Account? = null
    @Enumerated(value = EnumType.STRING)
    var linkType: LinkType = LinkType.SLACK
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

class AccountLinkResponse {
    var id: Long = 0
    var externalId: String? = null
    var token: String? = null
    var account: AccountResponse? = null
    var linkType: LinkType = LinkType.SLACK
    var created: Timestamp? = null
    var updated: Timestamp? = null
}

fun AccountLink.response(withTokens: Boolean = false): AccountLinkResponse {
    val self = this
    return AccountLinkResponse().apply {
        this.id = self.id
        this.externalId = self.externalId
        this.token = self.token
        this.account = self.account?.response(withTokens, false, false)
        this.linkType = self.linkType
        this.created = self.created
        this.updated = self.updated
    }
}