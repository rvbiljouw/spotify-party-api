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
import uk.bipush.party.model.Party
import uk.bipush.party.model.PartyMember


@WebSocket
class PartyHandler {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    private val connections: BiMap<Session, PartyMember> = HashBiMap.create()


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
        val wsRequest: WSRequest = mapper.readValue(message)
        when (wsRequest.opcode) {
            "join_party" -> handleJoinParty(user, wsRequest.body)
            "create_party" -> handleCreateParty(user, wsRequest.body)
        }
    }

    private fun handleJoinParty(user: Session, body: String) {
        val request: JoinPartyRequest = mapper.readValue(body)
        val existingMember = PartyMember.finder.query()
                .where()
                .eq("party.id", request.id)
                .eq("displayName", request.displayName)
                .findUnique()
        if (existingMember != null) {
            user.remote.sendString(mapper.writeValueAsString(WSResponse(true, PartyJoinedResponse(existingMember.party!!))))
        } else {
            val party = Party.finder.byId(request.id)
            if (party != null) {
                val member = PartyMember().apply {
                    this.party = party
                    this.displayName = request.displayName
                }
                member.save()
                connections.put(user, member)
                user.remote.sendString(mapper.writeValueAsString(WSResponse(true, PartyJoinedResponse(party))))
            } else {
                user.remote.sendString(mapper.writeValueAsString(WSResponse(false, "No such party.")))
            }
        }
    }

    private fun handleCreateParty(user: Session, body: String) {
//        val request: CreatePartyRequest = mapper.readValue(body)
//        val party: Party = Party().apply {
//            this.name = request.name
//        }
    }

}

data class PartySession(val session: Session, val party: Party)

data class WSRequest(val opcode: String, val body: String)

data class WSResponse<T>(val success: Boolean, val body: T)

data class JoinPartyRequest(val id: Long, val displayName: String)

data class PartyJoinedResponse(val party: Party)

data class AuthRequest(val email: String)