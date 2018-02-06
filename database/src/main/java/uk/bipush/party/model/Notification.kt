package uk.bipush.party.model

import io.ebean.Finder
import io.ebean.Model
import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.Index
import org.joda.time.DateTime
import javax.persistence.*

enum class NotificationAction {
    FOLLOWED
}

@Entity
class Notification: Model() {

    companion object {
        val finder = Finder<Long, Notification>(Notification::class.java)
    }

    @Id
    var id: Long? = null
    @ManyToOne
    var account: Account? = null
    @ManyToOne
    var interactingAccount: Account? = null
    var text: String? = null
    @Index
    @Enumerated(value = EnumType.STRING)
    var action: NotificationAction? = null
    @Index
    var read: Boolean = false
    @CreatedTimestamp
    var created: DateTime? = null

}

class NotificationResponse {
    var id: Long? = null
    var account: AccountResponse? = null
    var interactingAccount: AccountResponse? = null
    var text: String? = null
    var action: NotificationAction? = null
    var read: Boolean = false
    var created: DateTime? = null
}

fun Notification.response(withChildren: Boolean = true): NotificationResponse {
    val self = this

    return NotificationResponse().apply {
        this.id = self.id
        this.text = self.text
        this.action = self.action
        this.read = self.read
        this.created = self.created

        if (withChildren) {
            this.account = self.account?.response(false, false)
            this.interactingAccount = self.interactingAccount?.response(false, false)
        }
    }
}