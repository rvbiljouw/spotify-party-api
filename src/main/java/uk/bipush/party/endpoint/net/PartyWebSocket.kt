package uk.bipush.party.endpoint.net

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.collect.BiMap
import com.google.common.collect.EvictingQueue
import com.google.common.collect.HashBiMap
import com.sun.org.apache.xpath.internal.operations.Bool
import io.ebean.Expr
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketException
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import uk.bipush.party.model.Account
import uk.bipush.party.model.AccountType
import uk.bipush.party.model.Party
import uk.bipush.party.model.response
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.queue.response
import java.util.*


@WebSocket
class PartyWebSocket {

    companion object {
        private val logger = LoggerFactory.getLogger(PartyWebSocket::class.java)

        private val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val MAX_CHAT_MESSAGE_CACHE_SIZE = 50

        private val connections: BiMap<Session, Account> = HashBiMap.create()
        private val chatCache: HashMap<Party, Queue<ChatMessage>> = HashMap()


        fun sendQueueUpdate(partyQueue: PartyQueue, accounts: Set<Account>) {
            val json = mapper.writeValueAsString(partyQueue.response(false))

            val msg = mapper.writeValueAsString(WSMessage("QUEUE_UPDATE", json))

            sendMessage(msg, accounts)
        }

        fun sendPartyUpdate(party: Party, accounts: Set<Account>) {
            val json = mapper.writeValueAsString(party.response(false))

            val msg = mapper.writeValueAsString(WSMessage("PARTY_UPDATE", json))

            sendMessage(msg, accounts)
        }

        fun sendChatMessage(chatMessage: ChatMessage, accounts: Set<Account>) {
            val json = mapper.writeValueAsString(chatMessage)

            val msg = mapper.writeValueAsString(WSMessage("CHAT_MSG", json))

            sendMessage(msg, accounts)
        }

        fun sendChatMessage(chatMessages: List<ChatMessage>, accounts: Set<Account>) {
            val json = mapper.writeValueAsString(chatMessages)

            val msg = mapper.writeValueAsString(WSMessage("CHAT_MSGS", json))

            sendMessage(msg, accounts)
        }

        fun sendMessage(message: String, accounts: Set<Account>) {
            val inversed = connections.inverse()

            accounts.forEach { account ->
                val session = inversed.get(account)

                sendMessage(message, session)
            }
        }

        fun sendMessage(message: String, session: Session?) {
            try {
                if (session != null && session.isOpen) {
                    session.remote.sendString(message)
                }
            } catch (wse: WebSocketException) {
            } catch (t: Throwable) {
                logger.error("Error sending ws message", t)
            }
        }
    }

    @OnWebSocketConnect
    @Throws(Exception::class)
    fun onConnect(user: Session) {
    }

    @OnWebSocketClose
    fun onClose(user: Session, statusCode: Int, reason: String) {
        connections.remove(user)
    }

    @OnWebSocketMessage
    fun onMessage(user: Session, message: String) {
        val wsRequest: WSMessage = mapper.readValue(message)
        if (wsRequest.opcode == null || wsRequest.body == null) {
            return
        }

        val account = connections[user]

        if (wsRequest.opcode == "AUTH") {
            handleAuth(user, wsRequest.body)
        } else if (wsRequest.opcode == "PING") {
            handlePing(user, wsRequest.body)
        } else {
            if (account == null) {
                sendMessage(mapper.writeValueAsString(WSMessage("ERROR", "Forbidden")), user)
            } else {
                when (wsRequest.opcode) {
                    "CHAT" -> handleChat(user, wsRequest.body, account)
                    "VIEW_PARTY" -> handleViewParty(user, wsRequest.body, account)
                }
            }
        }
    }

    private fun handlePing(user: Session, body: String) {
        user.remote.sendString(mapper.writeValueAsString(WSMessage("PONG", "")))
    }

    private fun handleViewParty(user: Session, body: String, account: Account) {
        val partyId = body.toLong()

        val party = Party.finder.byId(partyId)

        if (party != null) {
            val messages = chatCache[party]

            if (messages != null) {
                sendChatMessage(messages.toList(), setOf(account))
            }
        } else {
            sendMessage(mapper.writeValueAsString(WSMessage("ERROR", "Unable to find party")), user)
        }
    }

    private fun handleChat(user: Session, body: String, account: Account) {
        val request: ChatRequest = mapper.readValue(body)

        val party = Party.finder.byId(request.partyId)

        if (party != null) {
            var messages = chatCache[party]
            if (messages == null) {
                messages = EvictingQueue.create(MAX_CHAT_MESSAGE_CACHE_SIZE)
            }
            chatCache.putIfAbsent(party, messages!!)

            val message = ChatMessage(account.displayName ?: "Guest",
                    request.message,
                    party.owner == account,
                    account.accountType == AccountType.STAFF,
                    false,
                    DateTime.now()
            )

            messages.add(message)

            sendChatMessage(message, party.members)
        } else {
            sendMessage(mapper.writeValueAsString(WSMessage("ERROR", "Unable to send message, can't find party")), user)
        }
    }

    private fun handleAuth(user: Session, body: String) {
        val request: AuthRequest = mapper.readValue(body)

        val account = Account.finder.query().where()
                .eq("id", request.userId)
                .findUnique()

        if (account?.loginToken != null && account.loginToken == request.loginToken) {
            connections.forcePut(user, account)

            sendMessage(mapper.writeValueAsString(WSMessage("AUTH", "true")), user)
        } else {
            user.disconnect()

            sendMessage(mapper.writeValueAsString(WSMessage("AUTH", "false")), user)
        }
    }

}

data class ChatMessage(val sender: String, val message: String, val isOwner: Boolean, val isStaff: Boolean, val isServer: Boolean, val timestamp: DateTime)

data class ChatRequest(val message: String, val partyId: Long)

data class WSMessage(val opcode: String?, val body: String?)

data class AuthRequest(val userId: Int, val loginToken: String)