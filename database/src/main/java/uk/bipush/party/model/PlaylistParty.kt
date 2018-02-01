package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*

@Entity
class PlaylistParty : Model() {

    companion object {
        val finder: Finder<Long, PlaylistParty> = Finder(PlaylistParty::class.java)
    }

    @Id
    var id: Long = 0
    @OneToOne
    var party: Party? = null
    @Index
    var playlistId: String? = null
    var playlistOwnerId: String? = null
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}