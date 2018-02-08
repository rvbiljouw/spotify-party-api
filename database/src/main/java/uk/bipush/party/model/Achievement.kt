package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Achievement: Model() {

    companion object {
        val finder: Finder<Long, Achievement> = Finder(Achievement::class.java)

        fun getJoinedAchievement(): Achievement {
            var achievement = finder.query().where().eq("name", "Joined").findOne()
            if (achievement == null) {
                achievement = Achievement().apply {
                    this.name = "Joined"
                    this.description = "Awarded when a user signs up"
                    this.badgeUrl = "https://i.imgur.com/eXwddgV.png"
                }

                achievement.save()
            }

            return achievement
        }
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