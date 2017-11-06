package uk.bipush.party.model

import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Subscription {
    @Id
    var id: String? = null
    var name: String? = ""
    var description: String? = ""
    var cost: BigDecimal? = BigDecimal.ZERO
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}