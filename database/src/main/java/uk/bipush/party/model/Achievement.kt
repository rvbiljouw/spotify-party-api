package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Achievement {

    companion object {
        val finder: Finder<Long, Achievement> = Finder(Achievement::class.java)

        fun getJoinedAchievement(): Achievement = finder.query().where().eq("name", "Joined").findOne()!!
    }

    @Id
    var id: Long? = null
    @Index
    var name: String? = ""
    var description: String? = ""
    var badgeUrl: String? = ""
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}