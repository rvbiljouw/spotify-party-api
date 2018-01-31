package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.SoftDelete
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*

enum class SongType {
    YOUTUBE, SPOTIFY
}

@Entity
class FavouriteSong : Model() {

    companion object {
        val finder: Finder<Long, FavouriteSong> = Finder(FavouriteSong::class.java)
    }

    @Id
    var id: Long = 0
    @ManyToOne
    var account: Account? = null
    @Enumerated(value = EnumType.STRING)
    var type: SongType? = null
    var songId: String? = null
    var artist: String? = null
    var title: String? = null
    @Index
    var uri: String? = null
    var thumbnail: String? = null
    var duration: Int = 0
    var previewUrl: String? = null
    var uploadedBy: String? = null
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null

}

data class FavouriteSongResponse(
        var id: Long = 0,
        var type: SongType? = null,
        var songId: String? = null,
        var artist: String? = null,
        var title: String? = null,
        var uri: String? = null,
        var thumbnail: String? = null,
        var duration: Int = 0,
        var previewUrl: String? = null,
        var uploadedBy: String? = null,
        var created: DateTime? = null,
        var updated: DateTime? = null
)

fun FavouriteSong.response(): FavouriteSongResponse {
    return FavouriteSongResponse(
            id = this.id,
            type = this.type,
            songId = this.songId,
            artist = this.artist,
            title = this.title,
            uri = this.uri,
            thumbnail = this.thumbnail,
            duration = this.duration,
            previewUrl = this.previewUrl,
            uploadedBy = this.uploadedBy,
            created = this.created,
            updated = this.updated
    )
}