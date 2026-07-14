package com.diprotec.inventario.worker

import android.util.Log
import androidx.work.ListenableWorker.Result
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
