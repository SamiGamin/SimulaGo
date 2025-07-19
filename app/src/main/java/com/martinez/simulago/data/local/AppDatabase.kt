package com.martinez.simulago.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date
class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
@Database(entities = [SavedSimulation::class, PaymentEntry::class],  version = 2, exportSchema = false)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao
    abstract fun simulationDao(): SimulationDao
}
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Añade la columna 'isActiveCredit' a la tabla 'simulations'
        db.execSQL("ALTER TABLE simulations ADD COLUMN isActiveCredit INTEGER NOT NULL DEFAULT 0")



        // Crea la nueva tabla 'payments' con su clave foránea
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `simulationId` INTEGER NOT NULL,
                `paymentDate` INTEGER NOT NULL,
                `amountPaid` REAL NOT NULL,
                `extraToCapital` REAL NOT NULL,
                `insuranceAndOthers` REAL NOT NULL,
                `notes` TEXT,
                FOREIGN KEY(`simulationId`) REFERENCES `simulations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        db.execSQL("ALTER TABLE simulations ADD COLUMN startDate INTEGER DEFAULT NULL")
    }
}



