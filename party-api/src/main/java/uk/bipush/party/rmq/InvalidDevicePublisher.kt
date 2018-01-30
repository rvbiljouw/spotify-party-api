package uk.bipush.party.rmq

import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.model.Account
import uk.bipush.party.model.PartyMember
import uk.bipush.party.model.SpotifyAccount
import uk.bipush.party.model.response
import uk.bipush.party.util.DBUtils

object InvalidDevicePublisher {

    fun publishInvalidDevice(accountId: Long, device: String) {
        //.TODO rmq


        val spotifyAccount = SpotifyAccount.finder.query().where()
                .eq("account.id", accountId)
                .eq("device", device)
                .findUnique()

        if (spotifyAccount?.activeParty != null) {
            val membership = PartyMember.finder.query().where()
                    .eq("account.id", accountId)
                    .eq("party.id", spotifyAccount.activeParty?.id)
                    .findUnique()

            if (membership != null) {
                DBUtils.transactional({
                    membership.active = false
                    membership.update()

                    if (membership.party != null) {
                        membership.party!!.activeMemberCount--
                        membership.party!!.save()
                    }

                    spotifyAccount.activeParty = null
                    spotifyAccount.update()
                })

                PartyWebSocket.sendPartyUpdate(membership.party!!, membership.party!!.members)
            }
        }
    }
}