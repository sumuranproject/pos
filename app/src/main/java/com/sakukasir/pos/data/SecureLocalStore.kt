package com.sakukasir.pos.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.secureDataStore by preferencesDataStore("sakukasir_secure")

class SecureLocalStore(private val context: Context) {
    private val usernameKey = stringPreferencesKey("session_username")
    private val themeKey = stringPreferencesKey("theme")
    private val loggedInKey = booleanPreferencesKey("logged_in")

    val username: Flow<String?> = context.secureDataStore.data.map { it[usernameKey] }
    val theme: Flow<String> = context.secureDataStore.data.map { it[themeKey] ?: "light" }

    suspend fun saveSession(username: String) = context.secureDataStore.edit {
        it[usernameKey] = username
        it[loggedInKey] = true
    }
    suspend fun clearSession() = context.secureDataStore.edit {
        it.remove(usernameKey)
        it[loggedInKey] = false
    }
    suspend fun saveTheme(theme: String) = context.secureDataStore.edit { it[themeKey] = theme }
}
