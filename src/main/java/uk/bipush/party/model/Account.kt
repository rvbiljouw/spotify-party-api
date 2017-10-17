package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import java.sql.Timestamp
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Account : Model() {

    companion object {
        val finder: Finder<Long, Account> = Finder(Account::class.java)
    }

    @Id
    var id: Long = 0
    var spotifyId: String? = null
    var displayName: String? = null
    var accessToken: String? = null
    var refreshToken: String? = null
    var selectedDevice: String? = null
    @CreatedTimestamp
    var created: Timestamp? = null
    @UpdatedTimestamp
    var updated: Timestamp? = null
}