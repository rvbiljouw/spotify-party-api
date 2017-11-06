package uk.bipush.party.endpoint.response

import com.wrapper.spotify.models.Track

class TrackResponse {
    var id: String? = null
    var artist: String? = null
    var title: String? = null
    var uri: String? = null
    var album: String? = null
    var thumbnail: String? = null
    var duration: Int = 0
    var explicit: Boolean = false
    var popularity: Int = 0
    var previewUrl: String? = null
}

fun Track.fromSpotify(): TrackResponse {
    val self = this
    return TrackResponse().apply {
        this.id = self.id
        this.title = self.name
        this.uri = self.uri

        this.artist = self.artists.joinToString { it.name }
        this.album = self.album?.name
        this.thumbnail = self.album?.images?.firstOrNull()?.url
        this.duration = self.duration
        this.explicit = self.isExplicit
        this.popularity = self.popularity
        this.previewUrl = self.previewUrl
    }
}