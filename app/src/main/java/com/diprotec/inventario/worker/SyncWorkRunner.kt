package com.diprotec.inventario.worker

import android.util.Log
import androidx.work.ListenableWorker.Result
import com.diprotec.inventario.core.network.ApiException
import java.io.IOException
import retrofit2.HttpException

internal suspend fun runSyncWork(
    tag: String,
    onUnexpected: (Throwable) -> Result,
    block: suspend () -> Unit
): Result {
    return try {
        block()
        Result.success()
    } catch (e: IOException) {
        Log.w(tag, "Network error -> retry", e)
        Result.retry()
    } catch (e: ApiException) {
        when {
            e.httpCode != null && e.httpCode >= 500 -> {
                Log.w(tag, "API ${e.httpCode} -> retry", e)
                Result.retry()
            }

            e.httpCode != null && e.httpCode in 400..499 -> {
                Log.w(tag, "API ${e.httpCode} -> failure", e)
                Result.failure()
            }

            e.cause is IOException -> {
                Log.w(tag, "Network error -> retry", e)
                Result.retry()
            }

            else -> onUnexpected(e)
        }
    } catch (e: HttpException) {
        Log.w(
            tag,
            "HTTP ${e.code()} -> ${if (e.code() >= 500) "retry" else "failure"}",
            e
        )

        if (e.code() >= 500) {
            Result.retry()
        } else {
            Result.failure()
        }
    } catch (t: Throwable) {
        onUnexpected(t)
    }
}
