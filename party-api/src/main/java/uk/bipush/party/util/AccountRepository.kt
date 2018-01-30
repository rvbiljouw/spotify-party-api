package uk.bipush.party.util

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import uk.bipush.http.auth.model.AuthenticableRepository
import uk.bipush.party.model.LoginToken
import java.util.concurrent.TimeUnit

class AccountRepository : AuthenticableRepository {
    val cache: LoadingCache<String, LoginToken> = CacheBuilder.newBuilder()
            .expireAfterAccess(5L, TimeUnit.MINUTES)
            .build(object : CacheLoader<String, LoginToken>() {
                override fun load(p0: String?): LoginToken? {
                    return LoginToken.finder.query()
                            .where()
                            .eq("token", p0)
                            .findUnique()
                }
            })

    override fun lookup(token: String, roles: Array<String>, status: Array<String>): Any? {
        try {
            return LoginToken.finder.query()
                    .where()
                    .eq("token", token)
                    .findUnique()
        } catch (t: Throwable) {
            return null
        }
    }

}