package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import io.ebean.annotation.UpdatedTimestamp
import org.joda.time.DateTime
import javax.persistence.*

@Entity
class SpotifyAccount : Model() {

    companion object {
        val finder = Finder<Long, SpotifyAccount>(SpotifyAccount::class.java)
    }

    @Id
    var id: Long? = 0
    @Index
    var spotifyId: String? = ""
    @ManyToOne
    var activeParty: Party? = null
    @ManyToOne
    var account: Account? = null
    var displayName: String? = ""
    var accessToken: String? = null
    var refreshToken: String? = null
    var device: String? = ""
    @CreatedTimestamp
    var created: DateTime? = null
    @UpdatedTimestamp
    var updated: DateTime? = null
}

class SpotifyAccountResponse {
    var id: Long? = 0
    var spotifyId: String? = ""
    var activeParty: PartyResponse? = null
    var account: AccountResponse? = null
    var displayName: String? = ""
    var accessToken: String? = null
    var refreshToken: String? = null
    var device: String? = ""
    var created: DateTime? = null
    var updated: DateTime? = null
}

fun SpotifyAccount.response(withAccount: Boolean = false, withTokens: Boolean = false): SpotifyAccountResponse {
    val self = this

    return SpotifyAccountResponse().apply {
        this.id = self.id
        this.spotifyId = self.spotifyId
        this.activeParty = self.activeParty?.response(false, false)
        this.displayName = self.displayName
        this.device = self.device
        this.created = self.created
        this.updated = self.updated

        if (withTokens) {
            this.accessToken = self.accessToken
            this.refreshToken = self.refreshToken
        }

        if (withAccount) {
            this.account = self.account?.response(false, false)
        }
    }
}