package uk.bipush.party.endpoint.response

import com.wrapper.spotify.models.Artist
import com.wrapper.spotify.models.SimpleArtist

class ArtistResponse {
    var id: String? = null
    var href: String? = null
    var name: String? = null
    var uri: String? = null
    var urls: Map<String, String>? = emptyMap()
    var genres: List<String>? = emptyList()
}

fun Artist.fromSpotify(): ArtistResponse {
    val self = this
    return ArtistResponse().apply {
        this.id = self.id
        this.href = self.href
        this.name = self.name
        this.uri = self.uri
        this.urls = self.externalUrls?.externalUrls
        this.genres = self.genres
    }
}

fun SimpleArtist.fromSpotify(): ArtistResponse {
    val self = this
    return ArtistResponse().apply {
        this.id = self.id
        this.href = self.href
        this.name = self.name
        this.uri = self.uri
        this.urls = self.externalUrls?.externalUrls
    }
}