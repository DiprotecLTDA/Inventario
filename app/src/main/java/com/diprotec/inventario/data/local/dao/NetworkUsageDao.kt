package com.diprotec.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.diprotec.inventario.data.local.entity.NetworkUsageEntity

data class NetworkUsageGroupRow(
    val name: String,
    val totalBytes: Long,
    val callCount: Int
)

@Dao
interface NetworkUsageDao {

    @Insert
    suspend fun insert(log: NetworkUsageEntity)

    @Query(
        """
        SELECT COALESCE(SUM(totalBytes), 0)
        FROM network_usage_logs
        WHERE createdAt BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun totalBytesBetween(
        startMillis: Long,
        endMillis: Long
    ): Long

    @Query(
        """
        SELECT COUNT(*)
        FROM network_usage_logs
        WHERE createdAt BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun callCountBetween(
        startMillis: Long,
        endMillis: Long
    ): Int

    @Query(
        """
        SELECT 
            operation AS name,
            COALESCE(SUM(totalBytes), 0) AS totalBytes,
            COUNT(*) AS callCount
        FROM network_usage_logs
        WHERE createdAt BETWEEN :startMillis AND :endMillis
        GROUP BY operation
        ORDER BY totalBytes DESC
        """
    )
    suspend fun usageByOperation(
        startMillis: Long,
        endMillis: Long
    ): List<NetworkUsageGroupRow>

    @Query(
        """
        SELECT 
            source AS name,
            COALESCE(SUM(totalBytes), 0) AS totalBytes,
            COUNT(*) AS callCount
        FROM network_usage_logs
        WHERE createdAt BETWEEN :startMillis AND :endMillis
        GROUP BY source
        ORDER BY totalBytes DESC
        """
    )
    suspend fun usageBySource(
        startMillis: Long,
        endMillis: Long
    ): List<NetworkUsageGroupRow>

    @Query(
        """
        SELECT 
            endpoint AS name,
            COALESCE(SUM(totalBytes), 0) AS totalBytes,
            COUNT(*) AS callCount
        FROM network_usage_logs
        WHERE createdAt BETWEEN :startMillis AND :endMillis
        GROUP BY endpoint
        ORDER BY totalBytes DESC
        """
    )
    suspend fun usageByEndpoint(
        startMillis: Long,
        endMillis: Long
    ): List<NetworkUsageGroupRow>

    @Query("DELETE FROM network_usage_logs")
    suspend fun deleteAll()

    @Query("DELETE FROM network_usage_logs WHERE createdAt < :olderThanMillis")
    suspend fun deleteOlderThan(olderThanMillis: Long)
}