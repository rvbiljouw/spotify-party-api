package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import java.sql.Timestamp
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class Follower: Model() {

    companion object {
        val finder: Finder<Long, Follower> = Finder(Follower::class.java)
    }
    @Id
    var id: Long = 0
    @ManyToOne
    var follower: Account? = null
    @ManyToOne
    var following: Account? = null
    @CreatedTimestamp
    var created: Timestamp? = null
    @UpdatedTimestamp
    var updated: Timestamp? = null
}

class FollowerResponse {
    var id: Long = 0
    var follower: AccountResponse? = null
    var following: AccountResponse? = null
    var created: Timestamp? = null
    var updated: Timestamp? = null
}

fun Follower.response(): FollowerResponse {
    val self = this

    return FollowerResponse().apply {
        this.id = self.id
        this.follower = self.follower?.response(false, false)
        this.following = self.following?.response(false, false)
        this.created = self.created
        this.updated = self.updated
    }
}