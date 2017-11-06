package uk.bipush.party.task

import com.wrapper.spotify.models.Track
import uk.bipush.party.model.PartyType
import uk.bipush.party.util.Spotify

class SpotifyBot : PartyBot(PartyType.SPOTIFY) {
    private val botChannels: List<BotChannel> = listOf(
            BotChannel(name = "ASOT", description = "The best of Trance.", keywords = listOf(
                    "armin van buuren",
                    "aly fila",
                    "cosmic gate",
                    "ferry corsten"
            )),
            BotChannel(name = "EDM", description = "Top quality EDM.", keywords = listOf(
                    "martin garrix",
                    "hardwell",
                    "tiesto"
            ))
    )

    override fun getChannels(): List<BotChannel> {
        return botChannels
    }

    override fun getTracks(keyword: String): List<Song> {
        val results = Spotify.searchSongsByQueryM(keyword, 0, 10)
        return if (results != null && results.items.isNotEmpty()) {
            val songs = results.items
                    .map { ComparableSong(it) }
                    .toHashSet()
                    .sortedByDescending { it.track.popularity }
                    .take(5)
                    .toSet()
            songs.map { it.toSong() }
        } else {
            listOf()
        }
    }

}

data class ComparableSong(val track: Track) {

    override fun equals(other: Any?): Boolean {
        if (other is ComparableSong) {
            return track.externalIds.externalIds["isrc"] == other.track.externalIds.externalIds["isrc"]
                    || track.name == other.track.name
                    || track.name.split(" ").first().trim() == other.track.name.split(" ").first().trim()
        }
        return super.equals(other)
    }

    fun toSong(): Song {
        return Song(
                artist = track.artists.firstOrNull()?.name ?: "Unknown",
                title = track.name,
                duration = track.duration,
                thumbnail = track.album.images.firstOrNull()?.url ?: "",
                url = track.uri
        )
    }

    override fun hashCode(): Int {
        return track.hashCode()
    }

}