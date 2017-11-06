package uk.bipush.party.model

import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Achievement {
    @Id
    var id: Long? = null
    var name: String? = ""
    var description: String? = ""
    var badgeUrl: String? = ""
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}