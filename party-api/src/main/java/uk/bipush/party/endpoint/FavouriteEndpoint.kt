package uk.bipush.party.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ebean.Expr
import spark.Request
import spark.Route
import spark.route.HttpMethod
import uk.bipush.http.Endpoint
import uk.bipush.http.auth.Auth
import uk.bipush.http.response.Errors
import uk.bipush.http.response.error
import uk.bipush.party.model.*
import uk.bipush.party.util.DBUtils
import uk.bipush.party.util.Filter

class FavouriteEndpoint {

    companion object {
        val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JodaModule())
    }

    @field:Auth
    @field:Endpoint(uri = "/api/v1/favourites", method = HttpMethod.get)
    val getFavourites = Route { req, res ->
        val token: LoginToken? = req.attribute("account")
        val filters: List<Filter>? = req.attribute("filters")
        val limit = req.queryParams("limit")?.toInt()
        val offset = req.queryParams("offset")?.toInt()
        val sort = req.queryParamOrDefault("sort", "").toString()
        val order = req.queryParamOrDefault("order", "").toString()


        var query = DBUtils.applyFilters(FavouriteSong.finder.query().where(Expr.eq("account.id", token?.account?.id)), filters)
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
        results.list.map { x -> x.response() }
    }

    @field:Auth
    @field:Endpoint(method = HttpMethod.post, uri = "/api/v1/favourites/search")
    val search = Route { req, res ->
        val filters: List<Filter> = mapper.readValue(req.body())
        req.attribute("filters", filters)
        getFavourites.handle(req, res)
    }

    @field:Auth
    @field:Endpoint(uri = "/api/v1/favourites/add", method = HttpMethod.post)
    val addFavourite = Route { req, res ->
        val token: LoginToken? = req.attribute("account")
        val favourite: FavouriteSong = mapper.readValue(req.body())

        val existing = FavouriteSong.finder.query().where()
                .eq("account.id", token!!.account!!.id)
                .eq("songId", favourite.songId)
                .eq("type", favourite.type)
                .findOne()

        if (existing == null) {
            favourite.account = token.account
            favourite.save()
            favourite.response()
        } else {
            res.error(Errors.conflict)
        }
    }

    @field:Auth
    @field:Endpoint(uri = "/api/v1/favourites/:id", method = HttpMethod.delete)
    val deleteFavourite = Route { req, res ->
        val token: LoginToken? = req.attribute("account")
        val id = req.params(":id").toLong()

        val favourite = FavouriteSong.finder.byId(id)

        if (favourite != null) {
            favourite.delete()
            favourite.response()
        } else {
            res.error(Errors.notFound)
        }
    }

}