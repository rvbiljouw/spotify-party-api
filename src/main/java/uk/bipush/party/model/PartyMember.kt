package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*

enum class PartyMemberRank {
    VISITOR, MODERATOR, HOST
}

@Entity
class PartyMember : Model() {

    companion object {
        val finder = Finder<Long, PartyMember>(PartyMember::class.java)
    }

    @Id
    var id: Long? = null
    @Enumerated(value = EnumType.STRING)
    var rank: PartyMemberRank = PartyMemberRank.HOST
    @ManyToOne
    var party: Party? = null
    @ManyToOne
    var account: Account? = null
    var lastSeen: DateTime? = null
    @Index
    var active: Boolean = true
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}
class PartyMemberResponse() {
    var id: Long? = 0
    var rank: PartyMemberRank? = null
    var account: AccountResponse? = null
    var lastSeen: DateTime? = null
    var active: Boolean = false
    var created: DateTime? = null
    var updated: DateTime? = null
}

fun PartyMember.response(withChildren: Boolean = false): PartyMemberResponse {
    val self = this

    return PartyMemberResponse().apply {
        this.id = self.id
        this.rank = self.rank

        if (withChildren) {
            this.account = self.account?.response(false, false)
        }

        this.lastSeen = self.lastSeen
        this.active = self.active
        this.created = self.created
        this.updated = self.updated
    }
}