package com.sakukasir.pos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val date: String,
    val time: String,
    val itemsJson: String,
    val total: Long,
    val method: String,
    val cashier: String,
    val cashierId: String,
    val outlet: String,
    val discount: Long,
    val tax: Long,
    val taxPct: Int,
    val received: Long,
    val change: Long,
    val status: String,
    val syncStatus: String,
    val qrisProofJson: String? = null,
    val refundAmount: Long = 0,
    val refundMethod: String? = null,
    val refundReason: String? = null,
    val refundedAt: Long? = null,
    val refundedBy: String? = null,
    val refundedItemsJson: String = ""
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TransactionEntity)
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [TransactionEntity::class], version = 2, exportSchema = false)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

class OfflineStore(private val database: OfflineDatabase) {
    val transactions: Flow<List<TransactionEntity>> = database.transactionDao().observeAll()
    suspend fun save(item: TransactionEntity) = database.transactionDao().upsert(item)
    suspend fun delete(id: String) = database.transactionDao().delete(id)
}
