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
import spark.Route
import spark.Spark
import uk.bipush.party.endpoint.net.PartyWebSocket
import uk.bipush.party.handler.PartyHandler
import uk.bipush.party.model.*
import uk.bipush.party.util.DBUtils
import uk.bipush.party.util.Filter
import uk.bipush.party.util.JacksonResponseTransformer
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.servlet.MultipartConfigElement

class PartyEndpoint(val partyHandler: PartyHandler) : Endpoint {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    private val storage: Storage = StorageOptions.getDefaultInstance().service


    override fun init() {
        Spark.get("/api/v1/parties", getAll, JacksonResponseTransformer())
        Spark.post("/api/v1/parties", search, JacksonResponseTransformer())

        Spark.get("/api/v1/party", myParties, JacksonResponseTransformer())
        Spark.get("/api/v1/party/:id", getById, JacksonResponseTransformer())
        Spark.put("/api/v1/party", joinParty, JacksonResponseTransformer())
        Spark.delete("/api/v1/party", leaveParty, JacksonResponseTransformer())
        Spark.post("/api/v1/party", createParty, JacksonResponseTransformer())
        Spark.put("/api/v1/party/activate", changeActiveParty, JacksonResponseTransformer())
        Spark.post("/api/v1/party/:id/background", uploadPartyBackground, JacksonResponseTransformer())
        Spark.put("/api/v1/party/:id", updatePartySettings, JacksonResponseTransformer())
        Spark.delete("api/v1/party/:id/members/:memberId", kickPartyMember, JacksonResponseTransformer())
    }

    val getAll = Route { req, res ->
        val filters: List<Filter>? = req.attribute("filters")
        val limit = req.queryParams("limit")?.toInt()
        val offset = req.queryParams("offset")?.toInt()
        val sort = req.queryParamOrDefault("sort", "").toString()
        val order = req.queryParamOrDefault("order", "").toString()

        var query = DBUtils.applyFilters(Party.finder.query().where(Expr.eq("access", PartyAccess.PUBLIC)), filters)
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

    val search = Route { req, res ->
        val filters: List<Filter> = mapper.readValue(req.body())
        req.attribute("filters", filters)
        getAll.handle(req, res)
    }

    val getById = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val partyId: Long? = req.params(":id").toLong()
        val account = Account.finder.byId(userId)
        if (account != null) {
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
            res.status(403)
        }
    }

    val myParties = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val loginToken: String = req.queryParams("loginToken")
        val account = Account.finder.byId(userId)
        if (account != null) {
            val parties = Party.finder.query().where().eq("members.id", account.id).findList()

            MyPartiesResponse(account.activeParty?.response(false, false),
                    parties.map { p -> p.response(false, false) }.toSet())
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val joinParty = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)
        if (account != null) {
            val request: JoinPartyRequest = mapper.readValue(req.body())

            val party = Party.finder.byId(request.id)
            if (party != null) {
                if (account.activeParty != null && account.activeParty == party) {
                    party.response(false)
                } else {
                    DBUtils.transactional({
                        party.members.add(account)
                        party.activeMembers.add(account)
                        account.activeParty = party

                        party.update()
                        account.update()
                    })

                    PartyWebSocket.sendPartyUpdate(party, party.members)

                    partyHandler.onPartyJoin(account, party)

                    party.response(false)
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

    val leaveParty = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val partyId: Long? = req.queryParams("partyId")?.toLong()
        val remove: Boolean = req.queryParamOrDefault("remove", "false").toBoolean()
        val account = Account.finder.byId(userId)
        if (account != null) {
            val party = Party.finder.query()
                    .where()
                    .eq("id", partyId)
                    .eq("members.id", account.id)
                    .findUnique()

            if (party != null) {
                DBUtils.transactional({
                    party.activeMembers.remove(account)
                    if (remove) {
                        party.members.remove(account)
                    }
                    party.update()


                    if (account.activeParty == party) {
                        account.activeParty = null

                        account.update()
                    }
                })

                PartyWebSocket.sendPartyUpdate(party, party.members)

                party.response(false)
            } else {
                res.status(404)
                mapOf("error" to "Party not found.")
            }
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val createParty = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)
        if (account != null) {
            val request: CreatePartyRequest = mapper.readValue(req.body())

            val party = Party().apply {
                this.owner = account
                this.name = request.name
                this.description = request.description
                this.members = mutableSetOf(account)
            }

            DBUtils.transactional({
                party.save()

                account.activeParty = party

                account.update()
            })

            partyHandler.addParty(party.id)

            party.response(true)
        } else {
            res.status(403)
            mapOf("error" to "You're not logged in.")
        }
    }

    val changeActiveParty = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val partyId: Long? = req.queryParams("partyId").toLong()
        val account = Account.finder.byId(userId)
        if (account != null) {
            val party = Party.finder.query()
                    .where()
                    .eq("id", partyId)
                    .eq("members.id", account.id)
                    .findUnique()
            if (party != null) {
                account.activeParty = party
                account.save()

                PartyWebSocket.sendPartyUpdate(party, party.members)

                partyHandler.onPartyJoin(account, party)


                party.response(false, false)
            } else {
                res.status(404)
            }
        } else {
            res.status(403)
        }
    }

    val updatePartySettings = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)

        val partyId: Long? = req.params(":id").toLong()
        val party = Party.finder.byId(partyId)
        if (account != null && party != null && party.owner?.id == userId) {
            val req: UpdatePartyRequest = mapper.readValue(req.body())
            party.access = req.access ?: party.access
            party.name = req.name ?: party.name
            party.description = req.description ?: party.description
            party.update()

            party.response(false, true)
        } else {
            res.status(403)
        }
    }

    val uploadPartyBackground = Route { req, res ->
        val multipartConfigElement = MultipartConfigElement("/tmp/")
        req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)

        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)

        val partyId: Long? = req.params(":id").toLong()
        val party = Party.finder.byId(partyId)
        println(userId)
        println(partyId)
        if (account != null && party != null && party.owner?.id == userId) {
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

    val kickPartyMember = Route { req, res ->
        val userId: Long? = req.session().attribute("user_id") ?: 0
        val account = Account.finder.byId(userId)

        val partyId: Long? = req.params(":id").toLong()
        val party = Party.finder.byId(partyId)
        if (account != null && party != null && party.owner?.id == userId) {
            val memberId: Long = req.params(":memberId").toLong()
            val member: Account? = Account.finder.byId(memberId)
            println(party.members)


            DBUtils.transactional({
                party.activeMembers.remove(member)
                party.members.remove(member)
                party.update()


                if (member?.activeParty == party) {
                    member.activeParty = null

                    member.update()
                }
            })

            PartyWebSocket.sendPartyUpdate(party, party.members)

            party.response(false)
            party.members
        } else {
            println(partyId)
            println(userId)
            res.status(404)
        }
    }

    private fun getBlobFileName(hash: String, fileName: String): String {
        return "$hash-${System.currentTimeMillis()}-$fileName"
    }
}

data class CreatePartyRequest(val name: String, val description: String)

data class JoinPartyRequest(val id: Long)

data class MyPartiesResponse(val activeParty: PartyResponse?, val parties: Set<PartyResponse>?)

data class UpdatePartyRequest(val name: String?, val description: String?, val access: PartyAccess?)