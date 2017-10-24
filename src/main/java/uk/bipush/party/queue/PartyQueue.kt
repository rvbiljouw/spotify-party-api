package uk.bipush.party.queue

import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.model.*

class PartyQueue {
    companion object {

        fun forParty(party: Party, offset: Int = 0, limit: Int = 25): PartyQueue {
            val entries = PartyQueueEntry.finder.query()
                    .where()
                    .eq("party.id", party.id)
                    .eq("status", RequestStatus.IN_QUEUE)
                    .setFirstRow(offset)
                    .setMaxRows(limit)
                    .order().desc("votes")
                    .order().asc("id")
                    .findList()

            val nowPlaying = PartyQueueEntry.finder.query()
                    .where()
                    .eq("party.id", party.id)
                    .eq("status", RequestStatus.PLAYING)
                    .findUnique()

            return PartyQueue().apply {
                val list = PartyQueueEntry.finder.query()
                        .where()
                        .eq("party.id", party.id)
                        .findList()

                this.nowPlaying = nowPlaying
                this.entries = entries.toSet()
                this.party = party
            }
        }

        fun queueSong(account: Account, party: Party, title: String, artist: String, duration: Int, thumbnail: String, uri: String):
                PartyQueueEntry {
            val entry = PartyQueueEntry().apply {
                this.party = party
                this.member = account
                this.artist = artist
                this.title = title
                this.duration = duration
                this.thumbnail = thumbnail
                this.uri = uri
            }

            entry.save()

            PartyWebSocket.sendQueueUpdate(PartyQueue.forParty(party), party.members)

            return entry
        }
    }

    var nowPlaying: PartyQueueEntry? = null
    var entries: Set<PartyQueueEntry> = emptySet()
    var party: Party? = null
}

class PartyQueueResponse {
    var nowPlaying: PartyQueueEntryResponse? = null
    var entries: Set<PartyQueueEntryResponse> = emptySet()
    var party: PartyResponse? = null
}

fun PartyQueue.response(withTokens: Boolean = false): PartyQueueResponse {
    val self = this
    return PartyQueueResponse().apply {
        this.nowPlaying = self.nowPlaying?.response(withTokens)
        this.entries = self.entries.map { p -> p.response(withTokens) }.toSet()
        this.party = self.party?.response(withTokens)
    }
}