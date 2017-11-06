package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*

@Entity
class SpotifyAccount : Model() {

    companion object {
        val finder = Finder<Long, SpotifyAccount>(SpotifyAccount::class.java)
    }

    @Id
    var id: Long? = 0
    @Index
    var spotifyId: String? = ""
    @ManyToOne
    var account: Account? = null
    var displayName: String? = ""
    var accessToken: String? = null
    var refreshToken: String? = null
    var device: String? = ""
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}