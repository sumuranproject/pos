package com.pentolrebus.kasir.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureLocalStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(context,"secure_session",MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    fun saveSession(uid:String,username:String,role:String,businessId:String?,outletId:String?){prefs.edit().putString("session_uid",uid).putString("session_username",username).putString("session_role",role).putString("session_business",businessId).putString("session_outlet",outletId).apply()}
    fun clearSession(){prefs.edit().remove("session_uid").remove("session_username").remove("session_role").remove("session_business").remove("session_outlet").apply()}
    fun session(): Map<String,String?>? { val uid=prefs.getString("session_uid",null)?:return null; return mapOf("uid" to uid,"username" to prefs.getString("session_username",null),"role" to prefs.getString("session_role",null),"businessId" to prefs.getString("session_business",null),"outletId" to prefs.getString("session_outlet",null)) }
}
