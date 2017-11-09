package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.storage.Acl
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.common.hash.Hashing
import com.google.common.io.Files
import io.ebean.Expr
import spark.Request
import spark.Route
import spark.route.HttpMethod
import uk.bipush.http.Endpoint
import uk.bipush.http.auth.Auth
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.handler.PartyManager
import uk.bipush.party.model.*
import uk.bipush.party.util.DBUtils
import uk.bipush.party.util.Filter
import uk.bipush.party.util.PlayTarget
import uk.bipush.party.util.Spotify
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.servlet.MultipartConfigElement

class PartyEndpoint @Inject constructor() {

    companion object {
        private val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        private val storage: Storage = StorageOptions.getDefaultInstance().service

    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/party/:id")
            /* NOTE: Don't change the URI as it messes up auth */
    val getById = Route { req, res ->
        val partyId = req.params(":id")

        val token: LoginToken = req.attribute("account")
        val account: Account? = token.account
        if (account != null) {
            if (partyId != "active") {
                val party = Party.finder.query()
                        .where()
                        .eq("id", partyId)
                        .eq("members.id", account.id)
                        .findUnique()
                if (party != null) {
                    party.response()
                } else {
                    res.status(404)
                }
            } else {
                account.spotify?.activeParty?.response()
            }
        } else {
            res.status(403)
        }
    }

    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/parties")
    val getParties = Route { req, res ->
        val token: LoginToken? = getLoginToken(req)
        val filters: List<Filter>? = req.attribute("filters")
        val limit = req.queryParams("limit")?.toInt()
        val offset = req.queryParams("offset")?.toInt()
        val sort = req.queryParamOrDefault("sort", "").toString()
        val order = req.queryParamOrDefault("order", "").toString()

        val accessExpr = if (token?.account != null) {
            Expr.or(Expr.eq("access", PartyAccess.PUBLIC), Expr.eq("members.account.id", token.account?.id))
        } else {
            Expr.eq("access", PartyAccess.PUBLIC)
        }

        var query = DBUtils.applyFilters(Party.finder.query().where(accessExpr), filters)
                .setFirstRow(offset ?: 0)
                .setMaxRows(limit ?: 25)

        if (sort.isNotBlank()) {
            if (order.toLowerCase() == "desc") {
                query = query.orderBy().desc(sort)
            } else {
                query = query.orderBy().asc(sort)
            }
        }

        val results = query.findPagedList()
        results.loadCount()

        res.header("X-Max-Records", results.totalCount.toString())
        res.header("X-Offset", (offset ?: 0).toString())
        results.list.map { x -> x.response(true) }
    }

    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/parties")
    val search = Route { req, res ->
        val filters: List<Filter> = mapper.readValue(req.body())
        req.attribute("filters", filters)
        getParties.handle(req, res)
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.get, uri = "/api/v1/parties/mine")
    val myParties = Route { req, res ->
        val token: LoginToken = req.attribute("account")

        if (token.account != null) {
            val parties = Party.finder.query().where().eq("members.account.id", token.account?.id)
                    .setMaxRows(5).findList()

            MyPartiesResponse(token.account?.spotify?.activeParty?.response(false, false),
                    parties.map { p -> p.response(false, false) }.toSet())
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.put, uri = "/api/v1/parties/:id/members")
    val joinParty = Route { req, res ->
        val partyId: Long = req.params(":id").toLongOrNull() ?: 0
        val reconnect = req.queryParamOrDefault("reconnect", "false").toBoolean()

        val token: LoginToken = req.attribute("account")
        val account = token.account

        if (account != null) {
            val party = Party.finder.byId(partyId)
            if (party != null) {
                if (account.spotify?.activeParty != null && account.spotify?.activeParty == party && !reconnect) {
                    party.response(false)
                } else {
                    try {
                        var isNewMember = false
                        val member = DBUtils.transactional({

                            PartyMember.finder.query().where()
                                    .eq("account.id", account.id)
                                    .eq("active", true)
                                    .not(Expr.eq("party.id", partyId))
                                    .eq("party.type", PartyType.SPOTIFY)
                                    .findList().forEach {
                                it.active = false

                                it.update()
                            }

                            var partyMember = PartyMember.finder.query().where()
                                    .eq("party.id", partyId)
                                    .eq("account.id", account.id)
                                    .findUnique()
                            if (partyMember == null) {
                                partyMember = PartyMember().apply {
                                    this.rank = PartyMemberRank.VISITOR
                                    this.party = party
                                    this.account = account
                                    this.active = true
                                }
                                isNewMember = true

                                partyMember.save()

                                party.members.add(partyMember)
                                party.activeMemberCount++
                                party.update()
                            } else if (!partyMember.active) {
                                partyMember.active = true
                                partyMember.update()

                                party.activeMemberCount++
                                party.update()
                            }

                            if (party.type == PartyType.SPOTIFY && account.spotify != null) {
                                account.spotify?.activeParty = party
                                Spotify.pause(listOf(PlayTarget(account.id, account.spotify!!.accessToken!!, account.spotify?.device)))
                                account.spotify?.update()
                            } else if (account.spotify != null && account.spotify?.activeParty != null) {
                                account.spotify?.activeParty = null
                                Spotify.pause(listOf(PlayTarget(account.id, account.spotify!!.accessToken!!, account.spotify?.device)))
                                account.spotify?.update()
                            }

                            partyMember
                        })

                        PartyWebSocket.sendPartyUpdate(party, party.members)
                        PartyManager.managers[party.type]?.onMemberAdded(member!!, isNewMember)

                        party.response(false)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        party.response(false, false)
                    }
                }
            } else {
                res.status(404)
                mapOf("error" to "Party not found.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.delete, uri = "/api/v1/parties/:id/members")
    val leaveParty = Route { req, res ->
        val partyId: Long? = req.params(":id")?.toLong()
        val remove: Boolean = req.queryParamOrDefault("remove", "false").toBoolean()

        val token: LoginToken = req.attribute("account")
        val account = token.account

        if (account != null) {
            val membership = PartyMember.finder.query().where()
                    .eq("account.id", account.id)
                    .eq("party.id", partyId)
                    .findUnique()

            if (membership != null) {
                DBUtils.transactional({
                    if (!remove) {
                        membership.active = false
                        membership.update()
                    } else {
                        membership.delete()
                    }

                    if (account.spotify?.activeParty == membership.party) {
                        account.spotify?.activeParty = null

                        Spotify.pause(listOf(PlayTarget(account.id, account.spotify!!.accessToken!!, account.spotify?.device)))
                        account.spotify?.update()
                    }
                })

                PartyWebSocket.sendPartyUpdate(membership.party!!, membership.party!!.members)
                membership.party?.response(false)
            } else {
                res.status(404)
                mapOf("error" to "Party not found.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/party")
    val createParty = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val account = token.account

        if (account != null) {
            val request: CreatePartyRequest = mapper.readValue(req.body())

            val party = Party().apply {
                this.owner = account
                this.name = request.name
                this.description = request.description
                this.type = request.type
                this.access = request.access
            }

            val partyMember = PartyMember().apply {
                this.rank = PartyMemberRank.HOST
                this.party = party
                this.account = account
                this.active = true
            }

            DBUtils.transactional({
                party.save()
                partyMember.save()

                party.activeMemberCount = 1
                party.members.add(partyMember)
                party.update()

                if (party.type == PartyType.SPOTIFY) {
                    account.spotify?.activeParty = party
                    account.spotify?.update()
                }
            })

            PartyManager.managers[party.type]?.register(party)

            party.response(true)
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.put, uri = "/api/v1/parties/:id")
    val updatePartySettings = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val account = token.account

        val partyId: Long? = req.params(":id").toLong()
        val party = Party.finder.byId(partyId)
        if (account != null && party != null && party.owner?.id == account.id) {
            val updateReq: UpdatePartyRequest = mapper.readValue(req.body())
            party.access = updateReq.access ?: party.access
            party.name = updateReq.name ?: party.name
            party.description = updateReq.description ?: party.description
            party.update()

            party.response(false, true)
        } else {
            res.status(403)
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/parties/:id/background")
    val uploadPartyBackground = Route { req, res ->
        val multipartConfigElement = MultipartConfigElement("/tmp/")
        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)

        val token: LoginToken = req.attribute("account")
        val account = token.account

        val partyId: Long? = req.params(":id").toLong()
        val party = Party.finder.byId(partyId)

        if (account != null && party != null && party.owner?.id == account.id) {
            val filePart = req.raw().getPart("file")
            var fileName = filePart.submittedFileName
            val extension = fileName.split(".").last()

            val file = File("/tmp/${System.currentTimeMillis()}.tmp")
            filePart.write(file.name)
            if (file.exists()) {
                if (arrayOf("png", "jpg", "jpeg").contains(extension)) {
                    // If it's an image we need to scale it
                    val image = ImageIO.read(file)
                    val buffer = BufferedImage(1920, 1080, BufferedImage.SCALE_SMOOTH)
                    buffer.graphics.drawImage(image, 0, 0, 1920, 1080, null)
                    ImageIO.write(buffer, "jpg", file)
                    fileName = "${filePart.submittedFileName}.jpg"
                }

                val hash = Files.hash(file, Hashing.adler32()).toString()
                fileName = getBlobFileName(hash, fileName)

                val blob = storage.create(BlobInfo
                        .newBuilder("partify-storage", fileName)
                        .setAcl(listOf(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                        .setMetadata(mapOf("adlerHash" to hash)).build(),
                        file.inputStream())

                party.backgroundUrl = blob.mediaLink
                party.save()


                party.response()
            } else {
                res.status(500)
            }
        } else {
            res.status(403)
        }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.delete, uri = "/api/v1/parties/:id/members/:memberId")
    val kickPartyMember = Route { req, res ->
        val token: LoginToken = req.attribute("account")
        val account = token.account

        val partyId: Long? = req.params(":id").toLong()
        val party = Party.finder.byId(partyId)
        if (account != null && party != null && party.owner?.id == account.id) {
            val memberId: Long = req.params(":memberId").toLong()
            val member: PartyMember? = PartyMember.finder.byId(memberId)

            DBUtils.transactional({
                member?.delete()


                if (member?.account?.spotify?.activeParty == party) {
                    member.account?.spotify?.activeParty = null
                    member.account?.spotify?.update()
                }
            })

            PartyWebSocket.sendPartyUpdate(party, party.members)

            party.response(false)
        } else {
            res.status(404)
        }
    }

    private fun getBlobFileName(hash: String, fileName: String): String {
        return "$hash-${System.currentTimeMillis()}-$fileName"
    }

    private fun getLoginToken(req: Request): LoginToken? {
        return if (req.attributes().contains("account")) req.attribute("account") else null
    }
}

data class CreatePartyRequest(val type: PartyType, val access: PartyAccess, val name: String, val description: String)

data class MyPartiesResponse(val activeSpotifyParty: PartyResponse?, val parties: Set<PartyResponse>?)

data class UpdatePartyRequest(val name: String?, val description: String?, val access: PartyAccess?)