package com.pentolrebus.kasir.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecureLocalStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(context,"secure_session",MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    fun saveCredential(username:String, uid:String, pin:CharArray) { val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}; val hash=derive(pin,salt); prefs.edit().putString("uid:$username",uid).putString("salt:$username",b64(salt)).putString("hash:$username",b64(hash)).apply(); pin.fill('\u0000') }
    fun verify(username:String,pin:CharArray):String? { val uid=prefs.getString("uid:$username",null)?:return null; val s=prefs.getString("salt:$username",null)?:return null; val expected=prefs.getString("hash:$username",null)?:return null; val actual=b64(derive(pin,Base64.decode(s,Base64.NO_WRAP))); pin.fill('\u0000'); return if(MessageDigest.isEqual(actual.toByteArray(),expected.toByteArray())) uid else null }
    fun saveSession(uid:String,username:String,role:String,businessId:String?,outletId:String?){prefs.edit().putString("session_uid",uid).putString("session_username",username).putString("session_role",role).putString("session_business",businessId).putString("session_outlet",outletId).apply()}
    fun saveWorkerSession(username:String,uid:String,ownerUid:String,businessId:String,outletId:String){prefs.edit().putString("worker:$username","$uid|$ownerUid|$businessId|$outletId").apply()}
    fun workerSession(username:String):List<String>?{val v=prefs.getString("worker:$username",null)?:return null;val p=v.split("|");return if(p.size==4)p else null}
    fun removeWorker(username:String){prefs.edit().remove("worker:$username").remove("uid:$username").remove("salt:$username").remove("hash:$username").apply()}
    fun clearSession(){prefs.edit().remove("session_uid").remove("session_username").remove("session_role").remove("session_business").remove("session_outlet").apply()}
    fun session(): Map<String,String?>? { val uid=prefs.getString("session_uid",null)?:return null; return mapOf("uid" to uid,"username" to prefs.getString("session_username",null),"role" to prefs.getString("session_role",null),"businessId" to prefs.getString("session_business",null),"outletId" to prefs.getString("session_outlet",null)) }
    private fun derive(pin:CharArray,salt:ByteArray):ByteArray { val spec=PBEKeySpec(pin,salt,120_000,256); return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.also{spec.clearPassword()} }
    private fun b64(b:ByteArray)=Base64.encodeToString(b,Base64.NO_WRAP)
}
