package uk.bipush.party.task

import org.joda.time.DateTime
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyMember

class PartyUpdater : Runnable {

    val query = PartyMember.finder.query()
            .where()
            .eq("active", true)
            .lt("updated", DateTime.now().minusMinutes(1))
            .setFirstRow(0)
            .setMaxRows(25)

    override fun run() {
        val members = query.findPagedList()
        members.loadCount()

        val count = members.totalCount
        var offset = 0
        while (offset < count) {
            val results = query
                    .setFirstRow(offset)
                    .setMaxRows(25)
                    .findPagedList()

            results.list.forEach({ member ->
                member.active = false
                member.update()

                if (member.party != null) {
                    member.party!!.activeMemberCount--
                    member.party!!.update()

                    PartyWebSocket.sendPartyUpdate(member.party!!, member.party!!.members)
                }
            })
            offset += results.list.size
        }
    }

}