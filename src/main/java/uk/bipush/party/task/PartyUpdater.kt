package uk.bipush.party.task

import uk.bipush.party.model.Party

class PartyUpdater : Runnable {

    override fun run() {
        val tokens = Party.finder.query()
                .setFirstRow(0)
                .setMaxRows(25)
                .findPagedList()
        tokens.loadCount()

        val count = tokens.totalCount
        var offset = 0
        while (offset < count) {
            val results = Party.finder.query()
                    .setFirstRow(offset)
                    .setMaxRows(25)
                    .findPagedList()
            results.list.forEach({ acc ->
                acc.activeMemberCount = acc.members.size
                acc.update()
            })
            offset += results.list.size
        }
    }

}