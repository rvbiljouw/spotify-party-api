package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*

enum class RequestStatus {
    PLAYED, PLAYING, IN_QUEUE, CANCELLED
}

@Entity
class PartyQueueEntry: Model() {

    companion object {
        val finder: Finder<Long, PartyQueueEntry> = Finder(PartyQueueEntry::class.java)
    }

    @Id
    var id: Long = 0
    @ManyToOne
    var party: Party? = null
    @ManyToOne
    var member: Account? = null
    var artist: String? = ""
    var title: String? = ""
    var thumbnail: String? = ""
    var duration: Int = 0
    var uri: String? = ""
    var playedAt: Long = 0
    var votes: Int = 0
    @Enumerated(value = EnumType.STRING)
    var status: RequestStatus = RequestStatus.IN_QUEUE
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

class PartyQueueEntryResponse {
    var id: Long = 0
    var party: PartyResponse? = null
    var member: AccountResponse? = null
    var artist: String? = ""
    var title: String? = ""
    var thumbnail: String? = ""
    var uri: String? = ""
    var votes: Int = 0
    var playedAt: Long = 0
    var duration: Int = 0
    var status: RequestStatus? = null
    var created: DateTime? = null
    var updated: DateTime? = null
}

fun PartyQueueEntry.response(withTokens: Boolean = false): PartyQueueEntryResponse {
    val self = this
    return PartyQueueEntryResponse().apply {
        this.id = self.id
        this.party = self.party?.response(withTokens)
        this.member = self.member?.response(withTokens, false)
        this.artist = self.artist
        this.title = self.title
        this.thumbnail = self.thumbnail
        this.uri = self.uri
        this.votes = self.votes
        this.playedAt = self.playedAt
        this.duration = self.duration
        this.status = self.status
        this.created = self.created
        this.updated = self.updated
    }
}