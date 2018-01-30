package uk.bipush.party.handler

import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyMember
import uk.bipush.party.model.PartyType

interface PartyManager : Runnable {

    fun onMemberAdded(member: PartyMember, isNewMember: Boolean)

    fun onMemberRemoved(member: PartyMember)

    companion object {
        val managers: Map<PartyType, PartyManager> = mapOf(
                PartyType.YOUTUBE to YouTubePartyManager,
                PartyType.SPOTIFY to SpotifyPartyManager
        )
    }

    fun register(party: Party)

    fun playNext(partyId: Long)
}
