package com.sakukasir.pos.data

import android.content.Context
import com.sakukasir.pos.domain.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Persistent local outbox. Items survive process death/restart and are removed only after sync succeeds. */
data class SyncQueueItem(
    val id: String,
    val type: String,
    val payload: String,
    val status: SyncStatus = SyncStatus.PENDING_SYNC
)

class OfflineSyncQueue(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sakukasir_sync_queue", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<SyncQueueItem>> = _items.asStateFlow()

    @Synchronized
    fun enqueue(item: SyncQueueItem) {
        _items.value = _items.value.filterNot { it.id == item.id } + item
        persist(_items.value)
    }

    @Synchronized
    fun markSynced(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
        persist(_items.value)
    }

    @Synchronized
    fun markError(id: String) {
        _items.value = _items.value.map { if (it.id == id) it.copy(status = SyncStatus.SYNC_ERROR) else it }
        persist(_items.value)
    }

    @Synchronized
    fun clear() {
        _items.value = emptyList()
        persist(emptyList())
    }

    private fun load(): List<SyncQueueItem> = runCatching {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    SyncQueueItem(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        payload = o.optString("payload"),
                        status = runCatching { SyncStatus.valueOf(o.optString("status")) }
                            .getOrDefault(SyncStatus.PENDING_SYNC)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun persist(items: List<SyncQueueItem>) {
        val array = JSONArray()
        items.forEach {
            array.put(JSONObject().apply {
                put("id", it.id)
                put("type", it.type)
                put("payload", it.payload)
                put("status", it.status.name)
            })
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "items"
    }
}
