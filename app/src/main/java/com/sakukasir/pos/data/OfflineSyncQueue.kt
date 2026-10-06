package com.sakukasir.pos.data

import com.sakukasir.pos.domain.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncQueueItem(val id: String, val type: String, val payload: String, val status: SyncStatus = SyncStatus.PENDING_SYNC)

class OfflineSyncQueue {
    private val _items = MutableStateFlow<List<SyncQueueItem>>(emptyList())
    val items: StateFlow<List<SyncQueueItem>> = _items.asStateFlow()

    fun enqueue(item: SyncQueueItem) {
        _items.value = _items.value.filterNot { it.id == item.id } + item
    }
    fun markSynced(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }
    fun markError(id: String) {
        _items.value = _items.value.map { if (it.id == id) it.copy(status = SyncStatus.SYNC_ERROR) else it }
    }
    fun clear() { _items.value = emptyList() }
}
