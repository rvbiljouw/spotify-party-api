package uk.bipush.party.util

import io.ebean.*

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

    fun <T> batcherate(
            getNextBatch: Query<T>,
            processFn: (List<T>) -> Boolean,
            startOffset: Int = 0,
            limit: Int = 5000
    ) {
        var offset = startOffset
        var pagedList: PagedList<T>? = null
        while(pagedList == null || pagedList.hasNext()) {
            pagedList = getNextBatch.setFirstRow(offset).setMaxRows(limit).findPagedList()
            pagedList.loadCount()

            if (processFn(pagedList.list)) {
                offset += pagedList.pageSize
            }
        }
    }

    fun <T> applyFilters(query: Query<T>, filters: List<uk.bipush.party.util.Filter>?): Query<T> {
        if (filters == null || filters.isEmpty()) {
            return query
        }

        val exprList = query.where()
        filters.forEach { exprList.add(it.compile()) }
        return exprList.query()
    }
}