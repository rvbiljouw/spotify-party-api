package uk.bipush.party.endpoint.response

import uk.bipush.party.util.SpotifyDevice

class SpotifyDeviceResponse {
    var id: String? = null
    var isActive: Boolean = false
    var isRestricted: Boolean = false
    var name: String = ""
    var type: String = ""
    var volumePercent: Double = 0.0
}

fun SpotifyDevice.response(): SpotifyDeviceResponse {
    val self = this

    return SpotifyDeviceResponse().apply {
        this.id = self.id
        this.isActive = self.isActive
        this.isRestricted = self.isRestricted
        this.name = self.name
        this.type = self.type
        this.volumePercent = self.volumePercent
    }
}