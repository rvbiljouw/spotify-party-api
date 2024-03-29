package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*
import javax.persistence.EnumType.*

enum class PartyQueueEntryStatus {
    PLAYED, PLAYING, IN_QUEUE, CANCELLED, SKIPPED
}

@Entity
class PartyQueueEntry : Model() {

    companion object {
        val finder: Finder<Long, PartyQueueEntry> = Finder(PartyQueueEntry::class.java)
    }

    @Id
    var id: Long = 0
    @ManyToOne
    var party: Party? = null
    @ManyToOne
    var member: Account? = null
    var songId: String? = ""
    var artist: String? = ""
    var title: String? = ""
    var thumbnail: String? = ""
    var duration: Int = 0
    var uri: String? = ""
    var uploadedBy: String? = ""
    var playedAt: Long = 0
    var votes: Int = 0
    var upvotes: Int = 0
    var downvotes: Int = 0
    var votesToSkip: Int = 0
    @Enumerated(value = STRING)
    var status: PartyQueueEntryStatus = PartyQueueEntryStatus.IN_QUEUE
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PartyQueueEntry

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "PartyQueueEntry(id=$id, party=$party, member=$member, artist=$artist, title=$title, thumbnail=$thumbnail, duration=$duration, uri=$uri, playedAt=$playedAt, votes=$votes, status=$status, created=$created, updated=$updated)"
    }


}

class PartyQueueEntryResponse {
    var id: Long = 0
    var party: PartyResponse? = null
    var member: AccountResponse? = null
    var songId: String? = ""
    var artist: String? = ""
    var title: String? = ""
    var thumbnail: String? = ""
    var uri: String? = ""
    var uploadedBy: String? = ""
    var votes: Int = 0
    var upvotes: Int = 0
    var downvotes: Int = 0
    var votesToSkip: Int = 0
    var playedAt: Long = 0
    var duration: Int = 0
    var status: PartyQueueEntryStatus? = null
    var created: DateTime? = null
    var updated: DateTime? = null
}

fun PartyQueueEntry.response(withTokens: Boolean = false, withParty: Boolean = true): PartyQueueEntryResponse {
    val self = this
    return PartyQueueEntryResponse().apply {
        this.id = self.id
        if (withParty) {
            this.party = self.party?.response(false)
        }
        this.member = self.member?.response(false, false)
        this.songId = self.songId
        this.artist = self.artist
        this.title = self.title
        this.thumbnail = self.thumbnail
        this.uri = self.uri
        this.uploadedBy = self.uploadedBy
        this.votes = self.votes
        this.upvotes = self.upvotes
        this.downvotes = self.downvotes
        this.votesToSkip = self.votesToSkip
        this.playedAt = self.playedAt
        this.duration = self.duration
        this.status = self.status
        this.created = self.created
        this.updated = self.updated
    }
}