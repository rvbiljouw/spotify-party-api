package uk.bipush.party.util

import com.fasterxml.jackson.annotation.JsonProperty


object Spotify {
    val CLIENT_ID = System.getenv("SPOTIFY_CLIENT_ID")
    val CLIENT_SECRET = System.getenv("SPOTIFY_CLIENT_SECRET")
}

data class SpotifyUser(val id: String, @field:JsonProperty("display_name") val displayName: String)

data class SpotifyToken(@field:JsonProperty("access_token") val accessToken: String, @field:JsonProperty("refresh_token") val refreshToken: String)