package uk.bipush.party.util

import io.ebean.Ebean
import io.ebean.Transaction
import io.ebean.TxIsolation

object DBUtils {
    fun <T> transactional(
            func: () -> T,
            isolation: TxIsolation = TxIsolation.DEFAULT,
            rollbackFor: Set<Class<out Throwable>> = emptySet(),
            noRollbackFor: Set<Class<out Throwable>> = emptySet()
    ): T? {
        var txn: Transaction? = null
        try {
            txn = Ebean.getServer(null).beginTransaction(isolation)

            val res = func()

            txn.commit()

            return res
        } catch (t: Throwable) {
            t.printStackTrace()

            if ((noRollbackFor.isEmpty() || !noRollbackFor.contains(t.javaClass)) &&
                    (rollbackFor.isEmpty() || rollbackFor.contains(t.javaClass))) {
                txn?.rollback()
            }
        }

        return null
    }
}