package uk.bipush.party.task

import io.ebean.Ebean
import uk.bipush.party.model.Account
import uk.bipush.party.util.DBUtils
import uk.bipush.party.util.Spotify

class OfflineDeviceUpdater: Runnable {
    override fun run() {
        DBUtils.batcherate<Account>(
                Account.finder.query().where()
                        .isNotNull("activeParty")
                        .isNotNull("selectedDevice")
                        .isNotNull("accessToken")
                        .query(),
                processBatch
        );
    }

    private val processBatch: (List<Account>) -> Boolean = { accounts ->
        val updated = accounts.map { account ->
            val devices = Spotify.getDevices(account.accessToken!!)

            val device = devices?.devices?.find { d -> d.id == account.selectedDevice }
            if (device == null || !device.isActive) {
                account.activeParty = null
                account
            } else {
                null
            }
        }.filterNotNull()

        Ebean.updateAll(updated)

        true
    }

}