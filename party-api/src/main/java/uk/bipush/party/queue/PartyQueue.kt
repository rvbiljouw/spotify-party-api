package uk.bipush.party.queue

import emoji4j.EmojiUtils
import org.joda.time.DateTime
import uk.bipush.party.endpoint.net.ChatMessage
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.handler.PartyManager
import uk.bipush.party.handler.SpotifyPartyManager
import uk.bipush.party.model.*

class PartyQueue {
    companion object {

        fun forParty(party: Party, offset: Int = 0, limit: Int = 25): PartyQueue {
            val entries = PartyQueueEntry.finder.query()
                    .where()
                    .eq("party.id", party.id)
                    .eq("status", PartyQueueEntryStatus.IN_QUEUE)
                    .setFirstRow(offset)
                    .setMaxRows(limit)
                    .order().desc("votes")
                    .order().asc("id")
                    .findList()

            val nowPlaying = PartyQueueEntry.finder.query()
                    .where()
                    .eq("party.id", party.id)
                    .eq("status", PartyQueueEntryStatus.PLAYING)
                    .findUnique()

            return PartyQueue().apply {
                this.nowPlaying = nowPlaying
                this.entries = entries.toSet()
                this.party = party
            }
        }

        fun queueSong(account: Account, party: Party, songId: String, title: String, artist: String, duration: Int,
                      thumbnail: String, uri: String, uploadedBy: String?):
                PartyQueueEntry {
            val entry = PartyQueueEntry().apply {
                this.party = party
                this.member = account
                this.songId = songId
                this.artist = EmojiUtils.htmlify(artist)
                this.title = EmojiUtils.htmlify(title)
                this.duration = duration
                this.thumbnail = thumbnail
                this.uri = uri
                this.uploadedBy = EmojiUtils.htmlify(uploadedBy ?: "")
            }

            entry.save()

            PartyWebSocket.sendQueueUpdate(PartyQueue.forParty(party), party.members)
            PartyWebSocket.sendChatMessage(ChatMessage("Awsum", "${account.displayName} queued up ${artist} - ${title}", false, false, true, DateTime.now()), party.members)


            return entry
        }

        fun voteSong(account: Account, party: Party, entry: PartyQueueEntry, upVote: Boolean, voteToSkip: Boolean): PartyQueueEntry? {
            var vote = PartyQueueVote.finder.query().where()
                    .eq("account.id", account.id)
                    .eq("entry.id", entry.id)
                    .findUnique()
            if (vote == null) {
                vote = PartyQueueVote().apply {
                    this.account = account
                    this.entry = entry
                    this.upvote = upVote
                    this.voteToSkip = voteToSkip
                }
                vote.save()

                if (upVote) {
                    entry.upvotes++
                } else {
                    entry.downvotes++
                }
                if (voteToSkip) {
                    entry.votesToSkip++
                }
                entry.votes = entry.upvotes - entry.downvotes

                if (entry.votesToSkip >= Math.ceil(party.activeMemberCount.toDouble() / 2.0)) {
                    entry.status = PartyQueueEntryStatus.SKIPPED

                    party.update()
                    entry.update()

                    PartyManager.managers[entry.party!!.type]?.playNext(entry.party!!.id)
                } else {
                    entry.update()
                }

                PartyWebSocket.sendQueueUpdate(PartyQueue.forParty(party), party.members)

                return entry
            } else {
                return null
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