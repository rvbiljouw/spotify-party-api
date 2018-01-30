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
    @field:Endpoint(uri = "/api/v1/favourites/add", method = HttpMethod.post)
    val addFavourite = Route { req, res ->
        val token: LoginToken? = req.attribute("account")
        val favourite: FavouriteSong = mapper.readValue(req.body())
        favourite.account = token?.account
        favourite.save()
        favourite.response()
    }

}