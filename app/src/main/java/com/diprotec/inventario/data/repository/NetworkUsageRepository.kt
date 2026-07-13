package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.dao.NetworkUsageDao
import com.diprotec.inventario.data.local.dao.NetworkUsageGroupRow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkUsageSummary(
    val todayBytes: Long,
    val last7DaysBytes: Long,
    val todayCallCount: Int,
    val averageBytesPerCallToday: Long,
    val byOperation: List<NetworkUsageGroupRow>,
    val bySource: List<NetworkUsageGroupRow>,
    val byEndpoint: List<NetworkUsageGroupRow>
)

@Singleton
class NetworkUsageRepository @Inject constructor(
    private val dao: NetworkUsageDao
) {

    suspend fun loadSummary(): NetworkUsageSummary {
        val now = System.currentTimeMillis()
        val todayStart = startOfTodayMillis()
        val sevenDaysStart = startOfDayMinusDays(6)

        val todayBytes = dao.totalBytesBetween(todayStart, now)
        val last7DaysBytes = dao.totalBytesBetween(sevenDaysStart, now)
        val todayCalls = dao.callCountBetween(todayStart, now)

        val average = if (todayCalls > 0) {
            todayBytes / todayCalls
        } else {
            0L
        }

        return NetworkUsageSummary(
            todayBytes = todayBytes,
            last7DaysBytes = last7DaysBytes,
            todayCallCount = todayCalls,
            averageBytesPerCallToday = average,
            byOperation = dao.usageByOperation(sevenDaysStart, now),
            bySource = dao.usageBySource(sevenDaysStart, now),
            byEndpoint = dao.usageByEndpoint(sevenDaysStart, now)
        )
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun deleteOlderThanDays(days: Int) {
        val threshold = startOfDayMinusDays(days)
        dao.deleteOlderThan(threshold)
    }

    private fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfDayMinusDays(days: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}