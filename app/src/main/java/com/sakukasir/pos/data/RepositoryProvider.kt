package com.sakukasir.pos.data

import android.content.Context
import androidx.room.Room

object RepositoryProvider {
    @Volatile private var instance: PosRepository? = null
    fun get(context: Context): PosRepository = instance ?: synchronized(this) {
        instance ?: run {
            val db = Room.databaseBuilder(context.applicationContext, OfflineDatabase::class.java, "sakukasir.db").build()
            LocalPosRepository(OfflineStore(db), OfflineSyncQueue(context.applicationContext)).also { instance = it }
        }
    }
}
