package com.sakukasir.pos.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RepositoryProvider {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN qrisProofJson TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN refundAmount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN refundMethod TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN refundReason TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN refundedAt INTEGER")
            db.execSQL("ALTER TABLE transactions ADD COLUMN refundedBy TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN refundedItemsJson TEXT NOT NULL DEFAULT ''")
        }
    }
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN outletAddress TEXT NOT NULL DEFAULT ''")
        }
    }
    @Volatile private var instance: PosRepository? = null
    fun get(context: Context): PosRepository = instance ?: synchronized(this) {
        instance ?: run {
            val db = Room.databaseBuilder(context.applicationContext, OfflineDatabase::class.java, "sakukasir.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
            LocalPosRepository(OfflineStore(db), OfflineSyncQueue(context.applicationContext)).also { instance = it }
        }
    }
}
