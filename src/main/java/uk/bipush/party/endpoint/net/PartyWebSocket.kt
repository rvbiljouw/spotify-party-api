package uk.bipush.party.endpoint.net

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import io.ebean.Expr
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import uk.bipush.party.model.Account
import uk.bipush.party.model.Party
import uk.bipush.party.model.response
import uk.bipush.party.queue.PartyQueue
import uk.bipush.party.queue.response


@WebSocket
class PartyWebSocket {

    companion object {
        private val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        private val connections: BiMap<Session, Account> = HashBiMap.create()


        fun sendQueueUpdate(partyQueue: PartyQueue, accounts: Set<Account>) {
            val json = mapper.writeValueAsString(partyQueue.response(false))

            val msg = mapper.writeValueAsString(WSMessage("QUEUE_UPDATE", json))

            val inversed = connections.inverse()

            accounts.forEach { account ->
                val session = inversed.get(account)
                if (session != null) {
                    session.remote.sendString(msg)
                }
            }
        }

        fun sendPartyUpdate(party: Party, accounts: Set<Account>) {
            val json = mapper.writeValueAsString(party.response(false))

            val msg = mapper.writeValueAsString(WSMessage("PARTY_UPDATE", json))

            val inversed = connections.inverse()

            accounts.forEach { account ->
                val session = inversed.get(account)
                if (session != null) {
                    session.remote.sendString(msg)
                }
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
        val account = connections[user]

        if (wsRequest.opcode == "auth") {
            handleAuth(user, wsRequest.body)
        } else {
            if (account == null) {
                user.remote.sendString("FORBIDDEN")
            } else {
                when (wsRequest.opcode) {
                    "chat" -> handleChat(user, wsRequest.body, account)
//                "join_party" -> handleJoinParty(user, wsRequest.body, account)
//                "create_party" -> handleCreateParty(user, wsRequest.body, account)
                }
            }
        }
    }

    private fun handleChat(user: Session, body: String, account: Account) {
        val request: ChatRequest = mapper.readValue(body)

        val party = Party.finder.byId(request.partyId)

    }

    private fun handleAuth(user: Session, body: String) {
        val request: AuthRequest = mapper.readValue(body)

        val account = Account.finder.query().where()
                .eq("id", request.userId)
                .eq("refreshToken", request.refreshToken)
                .findUnique()

        if (account != null) {
            connections.put(user, account)
        } else {
            user.disconnect()
        }
    }

}

data class ChatRequest(val message: String, val partyId: Long)

data class WSMessage(val opcode: String, val body: String)

data class WSResponse<T>(val success: Boolean, val body: T)

data class AuthRequest(val userId: Int, val refreshToken: String)