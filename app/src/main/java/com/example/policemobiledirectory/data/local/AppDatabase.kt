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
        OfficerEntity::class,
        PendingRegistrationEntity::class,
        AppIconEntity::class,
        NotificationEntity::class // ✅ Added NotificationEntity
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun officerDao(): OfficerDao
    abstract fun pendingRegistrationDao(): PendingRegistrationDao
    abstract fun appIconDao(): AppIconDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ✅ Migration 3 → 4: Add new column + new table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add new column to Employee table if missing
                    database.execSQL(
                        "ALTER TABLE employees ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 1"
                    )
                } catch (e: Exception) {
                    // Column might already exist, ignore
                }

                // Create new table for AppIconEntity if not exists
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

        // ✅ Migration 4 → 5: Add updatedAt column to employees table
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add updatedAt column to Employee table
                    database.execSQL(
                        "ALTER TABLE employees ADD COLUMN updatedAt INTEGER"
                    )
                } catch (e: Exception) {
                    // Column might already exist, ignore
                }
            }
        }

        // ✅ Migration 5 → 6: Add indexes for better query performance
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Create indexes for frequently queried columns
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_email ON employees(email)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_name ON employees(name)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_station ON employees(station)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_district ON employees(district)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_rank ON employees(rank)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_mobile1 ON employees(mobile1)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_mobile2 ON employees(mobile2)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_metalNumber ON employees(metalNumber)")
                } catch (e: Exception) {
                    // Indexes might already exist, ignore
                }
            }
        }
        
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
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_timestamp ON notifications(timestamp)")
            }
        }

        // ✅ Migration 7 → 8: Fix Schema Drift (add missing columns safely)
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add 'unit' to 'employees'
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN unit TEXT")
                } catch (e: Exception) {
                }

                // 2. Add 'unit' to 'pending_registrations'
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN unit TEXT")
                } catch (e: Exception) {
                }

                // 3. Add 'firebaseUid' to 'pending_registrations'
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN firebaseUid TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                }

                // 4. Add 'photoUrlFromGoogle' to 'pending_registrations'
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN photoUrlFromGoogle TEXT")
                } catch (e: Exception) {
                }
            }
        }

        // ✅ Migration 8 → 9: Power Search (add searchBlob and officers table)
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add searchBlob to employees
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN searchBlob TEXT NOT NULL DEFAULT ''")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_searchBlob ON employees(searchBlob)")
                } catch (e: Exception) {
                }

                // 2. Create officers table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS officers (
                        agid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT,
                        rank TEXT,
                        mobile TEXT,
                        landline TEXT,
                        station TEXT,
                        district TEXT,
                        unit TEXT,
                        photoUrl TEXT,
                        bloodGroup TEXT,
                        isHidden INTEGER NOT NULL DEFAULT 0,
                        searchBlob TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                
                // 3. Add indexes for officers
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_name ON officers(name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_district ON officers(district)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_rank ON officers(rank)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_unit ON officers(unit)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_searchBlob ON officers(searchBlob)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "employee_directory_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9) // ✅ Keep user data on update
                    .fallbackToDestructiveMigration() // ✅ Wipe data if migration fails
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
