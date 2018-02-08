package uk.bipush.party.bot.spotify

import com.wrapper.spotify.model_objects.specification.Playlist
import uk.bipush.party.api.QueueSongRequest
import uk.bipush.party.bot.Bot

class SpotifyPlaylistBot(val playlist: Playlist, botMother: SpotifyBotMother): Bot(botMother) {

    override fun getPlaylistId(): String {
        return playlist.id
    }

    override fun getPlaylistName(): String {
        return playlist.name
    }

    override fun getPlaylistDescription(): String {
        return playlist.description
    }

    override fun getPlaylistOwnerId(): String {
        return playlist.owner.id
    }

    override fun getNextSongs(): Set<QueueSongRequest> {
        val party = getCreateBotParty()

        var offfset = 0

        val limit = 50

        val requests: MutableList<QueueSongRequest> = mutableListOf()

        while (true) {
            val page = SpotifyBotMother.SPOTIFY_CLIENT
                    .getPlaylistsTracks(getPlaylistOwnerId(), getPlaylistId())
                    .offset(offfset)
                    .limit(50)
                    .build()
                    .execute()

            if (page.items.size == 0 || requests.size >= limit) {
                break
            }

           requests.addAll(page.items.filter { item ->
                shouldQueue(party, item.track.id)
            }.map { item ->
                            val id = item.track.id
                            val title = item.track.name
                            val url = item.track.uri
                            val artist = item.track.artists.joinToString { it.name }
                            val thumbnail = item.track.album?.images?.firstOrNull()?.url
                            val duration = item.track.durationMs

                            QueueSongRequest(id, artist, title, thumbnail
                                    ?: "", url, duration.toInt(), null)
            }.take(limit - requests.size))

            offfset += page.items.size
        }

        return requests.distinctBy { it.uri }.toSet()
    }

}