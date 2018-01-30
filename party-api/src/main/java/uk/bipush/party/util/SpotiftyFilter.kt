package uk.bipush.party.util

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class SpotifyFilter {

    enum class Type {
        NOT_EQUALS, EQUALS, CONTAINS, OR, STARTS_WITH
    }

    enum class SpotifyField {
        ALBUM, ARTIST, TRACK
    }

    var type: Type? = null
    var fieldName: SpotifyField? = null
    var value: String? = null
    var children: List<SpotifyFilter> = listOf()

    override fun toString(): String {
        return "Filter{" +
                "type=" + type +
                ", fieldName='" + fieldName + '\'' +
                ", value='" + value + '\'' +
                '}'
    }

    @JsonIgnore
    fun compile(): String {
        when (type) {

            Type.NOT_EQUALS -> return "${fieldName?.name?.toLowerCase()}:NOT ${value}"

            Type.EQUALS -> return "${fieldName?.name?.toLowerCase()}:${value}"

            Type.CONTAINS -> return "${fieldName?.name?.toLowerCase()}:*${value}*"

            Type.STARTS_WITH -> return "${fieldName?.name?.toLowerCase()}:${value}*"

            Type.OR -> {
                return children.map { it.compile() }.joinToString(" OR ")
            }

            else -> throw IllegalArgumentException(String.format("Unsupported expression type: %s", type))
        }
    }
}