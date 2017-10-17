package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

enum class PartyStatus {
    ONLINE, OFFLINE
}

@Entity
class Party: Model() {

    companion object {
        val finder: Finder<Long, Party> = Finder(Party::class.java)
    }

    @Id
    var id: Long? = 0
    @ManyToOne
    var account: Account? = null
    @OneToMany(mappedBy = "party")
    var members: MutableList<PartyMember> = mutableListOf()
    var name: String? = ""
    var description: String? = ""

    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}