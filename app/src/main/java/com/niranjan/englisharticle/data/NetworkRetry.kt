package com.niranjan.englisharticle.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Marks an HTTP response that is worth retrying (rate limiting or a transient
 * server error). Extends [IOException] so it is picked up by the default
 * retry predicate alongside genuine network failures.
 */
class RetryableHttpException(val code: Int, message: String) : IOException(message)

/** True for transient failures (network drops, 429s, 5xx) that a retry might recover from. */
fun Throwable.isTransientNetworkError(): Boolean = this is IOException

/**
 * Runs [block], retrying on transient failures with exponential backoff.
 * [CancellationException] is always rethrown immediately so coroutine
 * cancellation keeps working.
 */
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 1_000,
    maxDelayMs: Long = 8_000,
    factor: Double = 2.0,
    isRetryable: (Throwable) -> Boolean = Throwable::isTransientNetworkError,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    var lastError: Throwable? = null
    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            if (!isRetryable(error) || attempt == maxAttempts - 1) throw error
            lastError = error
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }
    throw lastError ?: IllegalStateException("retryWithBackoff exhausted without an error")
}
