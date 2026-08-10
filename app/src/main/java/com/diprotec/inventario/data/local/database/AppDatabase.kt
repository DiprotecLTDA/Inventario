package com.diprotec.inventario.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.diprotec.inventario.data.local.dao.LocationDao
import com.diprotec.inventario.data.local.dao.NetworkUsageDao
import com.diprotec.inventario.data.local.dao.ProductDao
import com.diprotec.inventario.data.local.dao.RuleDao
import com.diprotec.inventario.data.local.dao.SyncLogDao
import com.diprotec.inventario.data.local.dao.UnitMeasureDao
import com.diprotec.inventario.data.local.dao.UserDao
import com.diprotec.inventario.data.local.entity.LocationEntity
import com.diprotec.inventario.data.local.entity.NetworkUsageEntity
import com.diprotec.inventario.data.local.entity.ProductEntity
import com.diprotec.inventario.data.local.entity.RuleEntity
import com.diprotec.inventario.data.local.entity.SyncLogEntity
import com.diprotec.inventario.data.local.entity.UnitMeasureEntity
import com.diprotec.inventario.data.local.entity.UserEntity
import com.diprotec.inventario.data.local.dao.InventoryDao
import com.diprotec.inventario.data.local.entity.InventoryEntity
import com.diprotec.inventario.data.local.dao.InventoryItemDao
import com.diprotec.inventario.data.local.entity.InventoryItemEntity
import com.diprotec.inventario.data.local.dao.InventoryRemoteDao
import com.diprotec.inventario.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventario.data.local.entity.InventoryRemoteUserEntity
import com.diprotec.inventario.data.local.dao.AppErrorDao
import com.diprotec.inventario.data.local.entity.AppErrorEntity

@Database(
    entities = [
        UserEntity::class,
        InventoryEntity::class,
        InventoryItemEntity::class,
        RuleEntity::class,
        LocationEntity::class,
        ProductEntity::class,
        UnitMeasureEntity::class,
        InventoryRemoteEntity::class,
        InventoryRemoteUserEntity::class,
        SyncLogEntity::class,
        NetworkUsageEntity::class,
        AppErrorEntity::class
    ],
    version = 29,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun reglaDao(): RuleDao
    abstract fun ubicacionDao(): LocationDao
    abstract fun productoDao(): ProductDao
    abstract fun unidadMedidaDao(): UnitMeasureDao
    abstract fun inventarioRemotoDao(): InventoryRemoteDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun networkUsageDao(): NetworkUsageDao
    abstract fun appErrorDao(): AppErrorDao
}