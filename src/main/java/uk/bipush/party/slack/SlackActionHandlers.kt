package uk.bipush.party.slack

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import uk.bipush.party.endpoint.SlackAction
import uk.bipush.party.endpoint.SlackActionResponse
import uk.bipush.party.endpoint.SongSelect
import uk.bipush.party.model.Account
import uk.bipush.party.model.PartyQueueEntry
import uk.bipush.party.model.PartyQueueEntryStatus
import uk.bipush.party.model.response
import uk.bipush.party.queue.PartyQueue

object SlackActionHandlers {

    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun handleSongVote(action: SlackAction, upVote: Boolean): SlackActionResponse {
        val accountId = action.name?.toLong()
        val entryId = action.value?.toLong()

        val account = Account.finder.byId(accountId)
        val entry = PartyQueueEntry.finder.byId(entryId)

        val party = account?.spotify?.activeParty
        if (party != null && entry != null) {
            if (entry.status == PartyQueueEntryStatus.IN_QUEUE) {
                if (PartyQueue.voteSong(account, party, entry, upVote) != null) {
                    return SlackActionResponse("in_channel", false,
                            "${account.displayName} - ${if (upVote) "upvoted" else "downvoted"}" +
                                    " ${entry.title} - ${entry.artist ?: ""} in the ${party.name} party")
                }

                return SlackActionResponse("ephemeral", false, "You've already voted on this song?")
            } else {
                return SlackActionResponse("ephemeral", false,
                        "Unable to upvote song, can't find it in the queue")
            }
        } else {
            return SlackActionResponse("ephemeral", false,
                    "Unable to upvote song, do you have an active party?")
        }
    }

    fun handleSongSelect(action: SlackAction): SlackActionResponse {
        val accountId = action.name?.toLong()

        val account = Account.finder.byId(accountId)
        val songSelect: SongSelect = mapper.readValue(action.value!!)
        val party = account?.spotify?.activeParty
        if (party != null && songSelect.uri != null) {
            val entry = PartyQueue.queueSong(account, party, songSelect.title ?: "-", songSelect.artist ?: "-",
                    songSelect.duration ?: -0, songSelect.thumbnail ?: "", songSelect.uri!!)

            entry.response(false)
            return SlackActionResponse("in_channel", false,
                    "${songSelect.title} - ${songSelect.artist} has been queued in the ${party.name} party")
        } else {
            return SlackActionResponse("ephemeral", false, "Unable to queue song, do you have an active party?")
        }
    }
}