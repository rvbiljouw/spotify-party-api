package uk.bipush.party.endpoint.response

import com.wrapper.spotify.models.Track

class TrackResponse {
    var id: String? = null
    var href: String? = null
    var name: String? = null
    var uri: String? = null
    var album: AlbumResponse? = null
    var artists: List<ArtistResponse>? = emptyList()
    var markets: List<String>? = emptyList()
    var duration: Int = 0
    var explicit: Boolean = false
    var ids: Map<String, String>? = emptyMap()
    var urls: Map<String, String>? = emptyMap()
    var popularity: Int = 0
    var discNumber: Int = 0
    var previewUrl: String? = null
    var trackNumber: Int = 0
}

fun Track.fromSpotify(withChildren: Boolean = true): TrackResponse {
    val self = this
    return TrackResponse().apply {
        this.id = self.id
        this.href = self.href
        this.name = self.name
        this.uri = self.uri
        if (withChildren) {
            this.album = self.album.fromSpotify()
            this.artists = self.artists.map { a -> a.fromSpotify() }
        }
        this.markets = self.availableMarkets
        this.duration = self.duration
        this.explicit = self.isExplicit
        this.ids = self.externalIds?.externalIds
        this.urls = self.externalUrls?.externalUrls
        this.popularity = self.popularity
        this.discNumber = self.discNumber
        this.previewUrl = self.previewUrl
        this.trackNumber = self.trackNumber
    }
}