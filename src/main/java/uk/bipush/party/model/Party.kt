package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*

enum class PartyStatus {
    ONLINE, OFFLINE
}

enum class PartyAccess {
    PUBLIC, PRIVATE, PASSWORD
}

@Entity
class Party : Model() {

    companion object {
        val finder: Finder<Long, Party> = Finder(Party::class.java)
    }

    @Id
    var id: Long = 0
    @ManyToOne
    var owner: Account? = null
    @ManyToMany
    var members: MutableSet<Account> = mutableSetOf()
    @ManyToMany
    @JoinTable(name = "active_party_members")
    var activeMembers: MutableSet<Account> = mutableSetOf()
    var activeMemberCount: Int = 0
    var name: String? = ""
    var description: String? = ""
    var backgroundUrl: String? = ""
    var password: String? = ""
    @ManyToOne
    var nowPlaying: PartyQueueEntry? = null
    @Enumerated(value = EnumType.STRING)
    var status: PartyStatus? = PartyStatus.ONLINE
    @Enumerated(value = EnumType.STRING)
    var access: PartyAccess? = PartyAccess.PRIVATE
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Party

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

class PartyResponse {
    var id: Long = 0
    var owner: AccountResponse? = null
    var activeMembers: MutableSet<AccountResponse> = mutableSetOf()
    var members: MutableSet<AccountResponse> = mutableSetOf()
    var activeMemberCount: Int = 0
    var nowPlaying: PartyQueueEntryResponse? = null
    var name: String? = ""
    var description: String? = ""
    var backgroundUrl: String? = ""
    var access: PartyAccess? = null
    var status: PartyStatus? = null
    var created: DateTime? = null
    var updated: DateTime? = null
}

fun Party.response(withTokens: Boolean = false, withChildren: Boolean = true): PartyResponse {
    val self = this
    return PartyResponse().apply {
        this.id = self.id
        if (withChildren) {
            this.owner = self.owner?.response(withTokens, false)
            this.members = self.members.map { m -> m.response(false, false) }.toMutableSet()
            this.activeMembers = self.activeMembers.map { m -> m.response(false, false) }.toMutableSet()
        }

        this.activeMemberCount = self.members.size
        this.nowPlaying = self.nowPlaying?.response(false, false)
        this.name = self.name
        this.description = self.description
        this.access = self.access
        this.status = self.status
        this.backgroundUrl = self.backgroundUrl
        this.created = self.created
        this.updated = self.updated
    }
}