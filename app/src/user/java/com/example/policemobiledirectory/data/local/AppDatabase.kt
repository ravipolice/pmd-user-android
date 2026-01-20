package com.example.policemobiledirectory.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EmployeeEntity::class,
        PendingRegistrationEntity::class,
        AppIconEntity::class,
        NotificationEntity::class // ✅ Added NotificationEntity
    ],
    version = 7, // ✅ Incremented version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun pendingRegistrationDao(): PendingRegistrationDao
    abstract fun appIconDao(): AppIconDao
    abstract fun notificationDao(): NotificationDao // ✅ Added DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ... existing migrations ...
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {}
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_icons (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        iconUrl TEXT NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN updatedAt INTEGER")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_email ON employees(email)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_name ON employees(name)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_station ON employees(station)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_district ON employees(district)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_rank ON employees(rank)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_mobile1 ON employees(mobile1)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_mobile2 ON employees(mobile2)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_metalNumber ON employees(metalNumber)")
                } catch (e: Exception) {}
            }
        }

        // ✅ Migration 6 → 7: Add Notifications Table
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        targetKgid TEXT
                    )
                    """.trimIndent()
                )
                // Add index on timestamp for sorting
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_timestamp ON notifications(timestamp)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "employee_directory_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7) // ✅ Added Migration
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
