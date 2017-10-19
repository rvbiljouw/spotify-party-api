package uk.bipush.party.queue

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
                    .order().asc("votes")
                    .order().asc("id")
                    .findList()

            val nowPlaying = PartyQueueEntry.finder.query()
                    .where()
                    .eq("party.id", party.id)
                    .eq("status", RequestStatus.PLAYING)
                    .findUnique()

            return PartyQueue().apply {
                this.nowPlaying = nowPlaying
                this.entries = entries.toSet()
                this.party = party
            }
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