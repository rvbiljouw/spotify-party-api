package uk.bipush.party.bot

import uk.bipush.party.model.Account
import uk.bipush.party.model.AccountType
import uk.bipush.party.model.PartyType
import uk.bipush.party.model.PlaylistParty
import uk.bipush.party.util.DBUtils

abstract class BotMother<T : Bot>(val type: PartyType, val botEmail: String, val botDisplayName: String) {

    abstract fun createBot(ownerId: String, playlistId: String): T?

    abstract fun createNewBots(bots: MutableList<T>): List<T>

    fun createBots(): List<T> {
        return createNewBots(getExistingBots().toMutableList())
    }

    private fun getExistingBots(): List<T> {
        val bots = mutableListOf<T>()

        DBUtils.batcherate(
                PlaylistParty.finder.query().where()
                        .eq("party.type", type)
                        .eq("party.owner.accountType", AccountType.BOT)
                        .orderBy("id asc"),
                { parties ->
                    bots.addAll(parties
                            .filter { it.playlistId != null && it.playlistOwnerId != null }
                            .mapNotNull {
                                createBot(it.playlistOwnerId!!, it.playlistId!!)
                            })

                    true
                },
                0,
                50
        )

        return bots
    }

    fun getCreateBotAccount(): Account {
        var account = Account.finder.query().where()
                .eq("email", botEmail)
                .eq("accountType", AccountType.BOT)
                .findOne()

        if (account == null) {
            account = Account().apply {
                this.email = botEmail
                this.accountType = AccountType.BOT
                this.displayName = botDisplayName
            }

            account.save()
        }

        return account
    }
}