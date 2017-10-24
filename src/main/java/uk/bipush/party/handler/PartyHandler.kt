package uk.bipush.party.handler

import org.slf4j.LoggerFactory
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.model.Account
import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyQueueEntry
import uk.bipush.party.model.PartyQueueEntryStatus
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.util.PlayTarget
import uk.bipush.party.util.Spotify
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PartyHandler : Runnable {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val executorService = Executors.newScheduledThreadPool(10)

    private val newParties = CopyOnWriteArrayList<Long>()
    private val activeParties = CopyOnWriteArrayList<Long>()

    override fun run() {
        val started = newParties.map { partyId ->
            playNext(partyId)
            partyId
        }

        newParties.removeAll(started)
        activeParties.addAllAbsent(started)
    }

    private fun playNext(partyId: Long) {
        if (activeParties.contains(partyId) || newParties.contains(partyId)) {
            val party = Party.finder.byId(partyId)
            if (party == null) {
                executorService.schedule({ playNext(partyId) }, 5000L, TimeUnit.MILLISECONDS)
            } else {
                val queue = PartyQueue.forParty(party, 0, 2)

                if (!queue.entries.isEmpty() || queue.nowPlaying != null) {
                    if (queue.nowPlaying == null) {
                        val next = queue.entries.iterator().next()

                        val accounts = party.members.filter { account -> account.activeParty == party }
                        playSong(accounts, next, 0)

                        val now = System.currentTimeMillis()

                        next.playedAt = now
                        next.status = PartyQueueEntryStatus.PLAYING

                        next.update()

                        PartyWebSocket.sendQueueUpdate(queue, party.members)

                        executorService.schedule({ playNext(partyId) }, next.duration.toLong() + 1000, TimeUnit.MILLISECONDS)
                    } else {
                        val nowPlaying = queue.nowPlaying!!

                        if (nowPlaying.playedAt + nowPlaying.duration <= System.currentTimeMillis()) {
                            nowPlaying.status = PartyQueueEntryStatus.PLAYED
                            nowPlaying.update()

                            PartyWebSocket.sendQueueUpdate(queue, party.members)

                            playNext(partyId)
                        } else {
                            executorService.schedule({ playNext(partyId) }, 1000L, TimeUnit.MILLISECONDS)
                        }
                    }
                } else {
                    executorService.schedule({ playNext(partyId) }, 1000L, TimeUnit.MILLISECONDS)
                }
            }
        }
    }

    private fun playSong(accounts: List<Account>, entry: PartyQueueEntry, position: Long) {
        try {
            val playTargets = accounts.map { PlayTarget(it.accessToken!!, it.selectedDevice) }
            val uri = entry.uri

            if (uri != null) {
                Spotify.play(uri, playTargets, position)
            } else {
                logger.warn("Queue entry [${entry.id}] has null uri")
            }
        } catch (t: Throwable) {
            logger.error("Error playing next song", t)
        }
    }

    fun onPartyJoin(account: Account, party: Party) {
        val queue = PartyQueue.forParty(party, 0, 2)

        val nowPlaying = queue.nowPlaying
        if (nowPlaying != null) {
            playSong(listOf(account), nowPlaying, ((System.currentTimeMillis() - nowPlaying.playedAt)) + 500)
        }
    }

    fun addParty(partyId: Long) {
        newParties.add(partyId)
    }

    fun removeParty(partyId: Long) {
        activeParties.remove(partyId)
        newParties.remove(partyId)
    }
}