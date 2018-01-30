package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.SoftDelete
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.ManyToOne

enum class SongType {
    YOUTUBE, SPOTIFY
}

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
    var artist: String? = null
    var title: String? = null
    var uri: String? = null
    var thumbnail: String? = null
    var duration: Int = 0
    var previewUrl: String? = null
    var uploadedBy: String? = null
    @SoftDelete
    var deleted: Boolean = false
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null

}

data class FavouriteSongResponse(
        var type: SongType? = null,
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
            type = this.type,
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