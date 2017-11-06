package uk.bipush.party.handler

import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import uk.bipush.party.endpoint.net.ChatMessage
import uk.bipush.party.endpoint.net.Command
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.model.*
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.util.PlayTarget
import uk.bipush.party.util.Spotify
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object YouTubePartyManager : PartyManager {
    override fun register(party: Party) {
        if (!managedParties.contains(party.id)) {
            this.managedParties.add(party.id)
            playNext(party.id)
        }
    }

    private val executorService = Executors.newScheduledThreadPool(10)
    private val logger = LoggerFactory.getLogger(javaClass)

    private val managedParties = CopyOnWriteArrayList<Long>()

    override fun onMemberAdded(member: PartyMember) {
        val queue = PartyQueue.forParty(member.party!!, 0, 2)
        val nowPlaying = queue.nowPlaying
        if (nowPlaying != null) {
            PartyWebSocket.registerMemberCallback(member, Runnable {
                playSong(listOf(member), nowPlaying, ((System.currentTimeMillis() - nowPlaying.playedAt)) + 200)
            })
        }
        PartyWebSocket.sendChatMessage(ChatMessage("", "${member.account?.displayName} just joined the party.", false, false, true, DateTime.now()), member.party!!.members)
    }

    override fun onMemberRemoved(member: PartyMember) {

    }

    override fun run() {
        while (!Thread.currentThread().isInterrupted) {
            val parties = Party.finder.query()
                    .where()
                    .eq("status", PartyStatus.ONLINE)
                    .eq("type", PartyType.YOUTUBE)
                    .findList()
            parties.forEach {
                if (it.nowPlaying == null || !managedParties.contains(it.id)) {
                    managedParties.addIfAbsent(it.id)
                    playNext(it.id)
                }
            }

            Thread.sleep(30000)
        }
    }


    private fun playNext(partyId: Long) {
        if (managedParties.contains(partyId)) {
            val party = Party.finder.byId(partyId)
            if (party == null) {
                executorService.schedule({ playNext(partyId) }, 5000L, TimeUnit.MILLISECONDS)
            } else {
                val queue = PartyQueue.forParty(party, 0, 2)

                if (!queue.entries.isEmpty() || queue.nowPlaying != null) {
                    if (queue.nowPlaying == null) {
                        val next = queue.entries.iterator().next()

                        val accounts = party.members.filter { account -> account.active }
                        playSong(accounts, next, 0)

                        val now = System.currentTimeMillis()

                        next.playedAt = now
                        next.status = PartyQueueEntryStatus.PLAYING

                        next.update()

                        try {
                            party.nowPlaying = next

                            party.update()
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }

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

    private fun playSong(accounts: List<PartyMember>, entry: PartyQueueEntry, position: Long) {
        try {
            val uri = entry.uri
            if (uri != null) {
                val playCommand = Command("PLAY", mapOf("uri" to uri, "position" to position))
                PartyWebSocket.sendCommand(playCommand, accounts.toHashSet())
            } else {
                logger.warn("Queue entry [${entry.id}] has null uri")
            }
        } catch (t: Throwable) {
            logger.error("Error playing next song", t)
        }
    }

}