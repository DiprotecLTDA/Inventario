package com.diprotec.inventario.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.diprotec.inventario.BuildConfig
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.network.BaseUrlProvider
import com.diprotec.inventario.core.network.ApiCallExecutor
import com.diprotec.inventario.core.network.ConnectivityRetryInterceptor
import com.diprotec.inventario.core.network.DynamicBaseUrlInterceptor
import com.diprotec.inventario.core.network.NetworkUsageInterceptor
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.core.network.RawBodyLoggingInterceptor
import com.diprotec.inventario.data.local.dao.AppErrorDao
import com.diprotec.inventario.data.local.dao.InventoryDao
import com.diprotec.inventario.data.local.dao.InventoryItemDao
import com.diprotec.inventario.data.local.dao.InventoryRemoteDao
import com.diprotec.inventario.data.local.dao.LocationDao
import com.diprotec.inventario.data.local.dao.NetworkUsageDao
import com.diprotec.inventario.data.local.dao.ProductDao
import com.diprotec.inventario.data.local.dao.RuleDao
import com.diprotec.inventario.data.local.dao.SyncLogDao
import com.diprotec.inventario.data.local.dao.UnitMeasureDao
import com.diprotec.inventario.data.local.dao.UserDao
import com.diprotec.inventario.data.local.database.AppDatabase
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.repository.InventoryRemoteRepository
import com.diprotec.inventario.data.repository.InventoryRemoteRepositoryImpl
import com.diprotec.inventario.data.repository.InventoryRepository
import com.diprotec.inventario.data.repository.LocationRepository
import com.diprotec.inventario.data.repository.LocationRepositoryImpl
import com.diprotec.inventario.data.repository.ProductRepository
import com.diprotec.inventario.data.repository.ProductRepositoryImpl
import com.diprotec.inventario.data.repository.RuleRepository
import com.diprotec.inventario.data.repository.RuleRepositoryImpl
import com.diprotec.inventario.data.repository.UnitMeasureRepository
import com.diprotec.inventario.data.repository.UnitMeasureRepositoryImpl
import com.diprotec.inventario.data.repository.UserRepository
import com.diprotec.inventario.data.repository.UserRepositoryImpl
import com.diprotec.inventario.data.repository.AppErrorRepository
import com.diprotec.inventario.data.repository.AppErrorRepositoryImpl
import com.diprotec.inventario.service.AuthService
import com.diprotec.inventario.service.email.EmailConfiguration
import com.diprotec.inventario.service.email.KeystoreSmtpCredentialProvider
import com.diprotec.inventario.service.email.SmtpCredentialProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS network_usage_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    source TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    method TEXT NOT NULL,
                    endpoint TEXT NOT NULL,
                    url TEXT NOT NULL,
                    requestBytes INTEGER NOT NULL,
                    responseBytes INTEGER NOT NULL,
                    totalBytes INTEGER NOT NULL,
                    statusCode INTEGER,
                    durationMs INTEGER NOT NULL,
                    success INTEGER NOT NULL,
                    errorMessage TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_network_usage_logs_createdAt ON network_usage_logs(createdAt)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_network_usage_logs_source ON network_usage_logs(source)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_network_usage_logs_operation ON network_usage_logs(operation)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_network_usage_logs_endpoint ON network_usage_logs(endpoint)"
            )
        }
    }

    private val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Sin cambios de esquema en esta versión (bump de versión). No-op intencional.
        }
    }

    private val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventory_items_inventoryId ON inventory_items(inventoryId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventory_items_inventoryId_createdAt ON inventory_items(inventoryId, createdAt)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventory_items_sincronizado_remoteInventoryId_createdAt ON inventory_items(sincronizado, remoteInventoryId, createdAt)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventory_items_remoteInventoryId ON inventory_items(remoteInventoryId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventory_items_rutUsuario ON inventory_items(rutUsuario)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventory_items_inventoryId_barcode_ubicacionId ON inventory_items(inventoryId, barcode, ubicacionId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_productos_codigoSecundario ON productos(codigoSecundario)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_productos_descripcion ON productos(descripcion)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_productos_vigente ON productos(vigente)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_productos_rutEmpresa ON productos(rutEmpresa)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_sync_logs_sentAt ON sync_logs(sentAt)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_sync_logs_remoteInventoryId ON sync_logs(remoteInventoryId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_sync_logs_remoteInventoryId_sentAt ON sync_logs(remoteInventoryId, sentAt)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_sync_logs_result ON sync_logs(result)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_sync_logs_eventType ON sync_logs(eventType)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventarios_remotos_vigente ON inventarios_remotos(vigente)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventarios_remotos_fecha_hora ON inventarios_remotos(fecha, hora)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventarios_remotos_vigente_fecha_hora ON inventarios_remotos(vigente, fecha, hora)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventarios_remotos_rutAdministrador ON inventarios_remotos(rutAdministrador)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventarios_remotos_rutEmpresa ON inventarios_remotos(rutEmpresa)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventario_usuarios_remotos_inventarioId ON inventario_usuarios_remotos(inventarioId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventario_usuarios_remotos_rutUsuario ON inventario_usuarios_remotos(rutUsuario)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_inventario_usuarios_remotos_rutUsuario_inventarioId ON inventario_usuarios_remotos(rutUsuario, inventarioId)"
            )
        }
    }

    private val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_errors (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    fingerprint TEXT NOT NULL,
                    localState TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    lastSeenAt INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    module TEXT,
                    action TEXT NOT NULL,
                    message TEXT,
                    stackTrace TEXT,
                    endpoint TEXT,
                    screen TEXT,
                    inventoryId INTEGER,
                    workerName TEXT,
                    deviceManufacturer TEXT,
                    deviceModel TEXT,
                    deviceSerial TEXT,
                    appVersionName TEXT,
                    appVersionCode INTEGER,
                    attemptCount INTEGER NOT NULL DEFAULT 0,
                    sendError TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_app_errors_fingerprint ON app_errors(fingerprint)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_app_errors_localState ON app_errors(localState)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_app_errors_createdAt ON app_errors(createdAt)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_app_errors_fingerprint_lastSeenAt ON app_errors(fingerprint, lastSeenAt)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "control_acceso.db")
            .addMigrations(
                MIGRATION_25_26,
                MIGRATION_26_27,
                MIGRATION_27_28,
                MIGRATION_28_29
            )
            .fallbackToDestructiveMigrationFrom(*(1..24).toList().toIntArray())
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideReglaDao(db: AppDatabase): RuleDao = db.reglaDao()

    @Provides
    fun provideUbicacionDao(db: AppDatabase): LocationDao = db.ubicacionDao()

    @Provides
    fun provideProductoDao(db: AppDatabase): ProductDao = db.productoDao()

    @Provides
    fun provideUnidadMedidaDao(db: AppDatabase): UnitMeasureDao = db.unidadMedidaDao()

    @Provides
    fun provideInventoryDao(db: AppDatabase): InventoryDao = db.inventoryDao()

    @Provides
    fun provideInventoryItemDao(db: AppDatabase): InventoryItemDao = db.inventoryItemDao()

    @Provides
    fun provideInventarioRemotoDao(db: AppDatabase): InventoryRemoteDao =
        db.inventarioRemotoDao()

    @Provides
    fun provideSyncLogDao(db: AppDatabase): SyncLogDao = db.syncLogDao()

    @Provides
    fun provideNetworkUsageDao(db: AppDatabase): NetworkUsageDao =
        db.networkUsageDao()

    @Provides
    fun provideAppErrorDao(db: AppDatabase): AppErrorDao = db.appErrorDao()

    @Provides
    @Singleton
    fun provideInventoryRepository(
        inventoryDao: InventoryDao,
        inventoryItemDao: InventoryItemDao,
        productDao: ProductDao,
        locationDao: LocationDao,
        settings: SettingsManager
    ): InventoryRepository = InventoryRepository(
        inventoryDao = inventoryDao,
        inventoryItemDao = inventoryItemDao,
        productDao = productDao,
        locationDao = locationDao,
        settings = settings
    )

    @Provides
    @Singleton
    fun moshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun okHttp(
        connectivityRetryInterceptor: ConnectivityRetryInterceptor,
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
        networkUsageInterceptor: NetworkUsageInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(connectivityRetryInterceptor)
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(networkUsageInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(RawBodyLoggingInterceptor())
                }
            }
            .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://inventariowebapi.diprotec.cl/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()

    @Provides
    @Singleton
    fun api(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun userRepo(
        userDao: UserDao,
        api: ApiService,
        apiCallExecutor: ApiCallExecutor,
        settings: SettingsManager,
        headersBuilder: ProtectedHeadersBuilder
    ): UserRepository = UserRepositoryImpl(
        userDao = userDao,
        api = api,
        apiCallExecutor = apiCallExecutor,
        settings = settings,
        headersBuilder = headersBuilder
    )

    @Provides
    @Singleton
    fun reglaRepo(
        ruleDao: RuleDao,
        api: ApiService,
        apiCallExecutor: ApiCallExecutor,
        settings: SettingsManager,
        headersBuilder: ProtectedHeadersBuilder
    ): RuleRepository = RuleRepositoryImpl(
        ruleDao = ruleDao,
        api = api,
        apiCallExecutor = apiCallExecutor,
        settings = settings,
        headersBuilder = headersBuilder
    )

    @Provides
    @Singleton
    fun ubicacionRepo(
        locationDao: LocationDao,
        api: ApiService,
        apiCallExecutor: ApiCallExecutor,
        settings: SettingsManager,
        headersBuilder: ProtectedHeadersBuilder
    ): LocationRepository = LocationRepositoryImpl(
        locationDao = locationDao,
        api = api,
        apiCallExecutor = apiCallExecutor,
        settings = settings,
        headersBuilder = headersBuilder
    )

    @Provides
    @Singleton
    fun productoRepo(
        productDao: ProductDao,
        api: ApiService,
        apiCallExecutor: ApiCallExecutor,
        settings: SettingsManager,
        headersBuilder: ProtectedHeadersBuilder
    ): ProductRepository = ProductRepositoryImpl(
        productDao = productDao,
        api = api,
        apiCallExecutor = apiCallExecutor,
        settings = settings,
        headersBuilder = headersBuilder
    )

    @Provides
    @Singleton
    fun unidadMedidaRepo(
        unitMeasureDao: UnitMeasureDao,
        api: ApiService,
        apiCallExecutor: ApiCallExecutor,
        settings: SettingsManager,
        headersBuilder: ProtectedHeadersBuilder
    ): UnitMeasureRepository = UnitMeasureRepositoryImpl(
        unitMeasureDao = unitMeasureDao,
        api = api,
        apiCallExecutor = apiCallExecutor,
        settings = settings,
        headersBuilder = headersBuilder
    )

    @Provides
    @Singleton
    fun baseUrlProvider(settings: SettingsManager): BaseUrlProvider =
        object : BaseUrlProvider {
            override fun getBaseUrl(): String = settings.baseUrl.value
        }

    @Provides
    @Singleton
    fun authService(userDao: UserDao) = AuthService(userDao)

    @Provides
    @Singleton
    fun inventarioRemotoRepo(
        inventoryRemoteDao: InventoryRemoteDao,
        api: ApiService,
        apiCallExecutor: ApiCallExecutor,
        settings: SettingsManager,
        headersBuilder: ProtectedHeadersBuilder
    ): InventoryRemoteRepository = InventoryRemoteRepositoryImpl(
        dao = inventoryRemoteDao,
        api = api,
        apiCallExecutor = apiCallExecutor,
        settings = settings,
        headersBuilder = headersBuilder
    )

    @Provides
    @Singleton
    fun appErrorRepository(dao: AppErrorDao): AppErrorRepository =
        AppErrorRepositoryImpl(dao)

    @Provides
    @Singleton
    fun emailConfiguration(): EmailConfiguration = EmailConfiguration()

    @Provides
    @Singleton
    fun smtpCredentialProvider(
        @ApplicationContext context: Context
    ): SmtpCredentialProvider = KeystoreSmtpCredentialProvider(context)
}
