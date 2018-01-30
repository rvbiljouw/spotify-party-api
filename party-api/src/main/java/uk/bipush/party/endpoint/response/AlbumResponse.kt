package uk.bipush.party.endpoint.response

import com.wrapper.spotify.models.AlbumType
import com.wrapper.spotify.models.Image
import com.wrapper.spotify.models.SimpleAlbum

class AlbumResponse {
    var id: String? = null
    var href: String? = null
    var name: String? = null
    var uri: String? = null
    var type: AlbumType? = null
    var urls: Map<String, String>? = emptyMap()
    var markets: List<String>? = emptyList()
    var images: List<Image>? = emptyList()
}

fun SimpleAlbum.fromSpotify(): AlbumResponse {
    val self = this
    return AlbumResponse().apply {
        this.id = self.id
        this.href = self.href
        this.name = self.name
        this.uri = self.uri
        this.type = self.albumType
        this.urls = self.externalUrls?.externalUrls
        this.markets = self.availableMarkets
        this.images = self.images
    }

}