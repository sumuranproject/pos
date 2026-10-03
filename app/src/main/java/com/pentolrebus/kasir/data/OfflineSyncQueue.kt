package com.pentolrebus.kasir.data

import android.content.Context
import com.pentolrebus.kasir.domain.SyncStatus
import org.json.JSONArray
import org.json.JSONObject

class OfflineSyncQueue(context:Context){
 private val f=context.getFileStreamPath("sync-queue.json")
 @Synchronized fun enqueue(kind:String,id:String){val a=read();if((0 until a.length()).none{a.getJSONObject(it).getString("id")==id})a.put(JSONObject().put("kind",kind).put("id",id).put("status",SyncStatus.PENDING_SYNC.name));write(a)}
 @Synchronized fun mark(id:String,status:SyncStatus){val a=read();for(i in 0 until a.length()){val o=a.getJSONObject(i);if(o.getString("id")==id)o.put("status",status.name)};write(a)}
 fun pending():JSONArray=read()
 private fun read()=if(f.exists())JSONArray(f.readText()) else JSONArray()
 private fun write(a:JSONArray){f.writeText(a.toString())}
}
