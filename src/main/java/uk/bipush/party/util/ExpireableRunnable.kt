package uk.bipush.party.util

import com.google.common.util.concurrent.SimpleTimeLimiter
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class ExpireableRunnable(val name: String = "unnamed", val runnable: Runnable, val verbose: Boolean = false) : Runnable {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun run() {
        try {
            val limiter = SimpleTimeLimiter()
            if (verbose) {
                logger.info("Running task $name.")
            }
            limiter.callWithTimeout({ runnable.run() }, 60L, TimeUnit.SECONDS, true)
            if (verbose) {
                logger.info("Task $name completed.")
            }
        } catch (t: Throwable) {
            logger.error("Execution of task $name failed.", t)
        }
    }

}