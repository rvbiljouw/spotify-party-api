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

class PartyMemberResponse(p: PartyMember) {
    val id: Long? = p.id
    val rank: PartyMemberRank? = p.rank
    val account: AccountResponse? = p.account?.response()
    val lastSeen: DateTime? = p.lastSeen
    val active: Boolean = p.active
    val created: DateTime? = p.created
    val updated: DateTime? = p.updated
}