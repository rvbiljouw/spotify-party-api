package uk.bipush.party.util

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class SpotifyFilter {

    enum class Type {
        NOT_EQUALS, EQUALS, CONTAINS
    }

    enum class SpotifyField {
        ALBUM, ARTIST, TRACK
    }

    var type: Type? = null
    var fieldName: SpotifyField? = null
    var value: String? = null

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

            Type.CONTAINS -> return  "${fieldName?.name?.toLowerCase()}:*${value}*"

            else -> throw IllegalArgumentException(String.format("Unsupported expression type: %s", type))
        }
    }
}