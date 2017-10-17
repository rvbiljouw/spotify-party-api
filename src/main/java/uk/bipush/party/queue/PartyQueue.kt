package uk.bipush.party.queue

import uk.bipush.party.model.*

class PartyQueue {
    companion object {
        fun forParty(party: Party): PartyQueue {
            val entries = PartyQueueEntry.finder.query()
                    .where()
                    .eq("party.id", party.id)
                    .eq("status", RequestStatus.IN_QUEUE)
                    .order().asc("votes")
                    .order().asc("id")
                    .findList()

            return PartyQueue().apply {
                this.entries = entries.toSet()
                this.party = party
            }
        }
    }

    var entries: Set<PartyQueueEntry> = emptySet()
    var party: Party? = null
}

class PartyQueueResponse {
    var entries: Set<PartyQueueEntryResponse> = emptySet()
    var party: PartyResponse? = null
}

fun PartyQueue.response(withTokens: Boolean = false): PartyQueueResponse {
    val self = this
    return PartyQueueResponse().apply {
        this.entries = self.entries.map { p -> p.response(withTokens) }.toSet()
        this.party = self.party?.response(withTokens)
    }
}