package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class PartyQueueVote : Model() {

    companion object {
        val finder: Finder<Long, PartyQueueVote> = Finder(PartyQueueVote::class.java)
    }

    @Id
    var id: Long = 0
    @ManyToOne
    var account: Account? = null
    @ManyToOne
    var entry: PartyQueueEntry? = null
    var upvote: Boolean? = false
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}