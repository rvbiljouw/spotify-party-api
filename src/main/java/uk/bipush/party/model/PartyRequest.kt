package uk.bipush.party.model

import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne

enum class RequestStatus {
    PLAYED, IN_QUEUE, CANCELLED
}

@Entity
class PartyRequest: Model() {
    @Id
    var id: Long? = 0
    @ManyToOne
    var party: Party? = null
    @ManyToOne
    var member: PartyMember? = null
    var artist: String? = ""
    var title: String? = ""
    var thumbnail: String? = ""
    var uri: String? = ""
    var votes: Int = 0
    var index: Int = 0
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}