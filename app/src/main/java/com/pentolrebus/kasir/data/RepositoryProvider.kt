package com.pentolrebus.kasir.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.pentolrebus.kasir.util.Diagnostics

object RepositoryProvider {
 fun create(context:Context, diagnostics:Diagnostics):PosRepository {
  val secure=SecureLocalStore(context)
  return try {
   val app=FirebaseApp.initializeApp(context)
   if(app!=null) FirebasePosRepository(FirebaseAuth.getInstance(app),FirebaseDatabase.getInstance(app).reference,secure){t,m->diagnostics.log(runCatching{com.pentolrebus.kasir.util.LogTag.valueOf(t)}.getOrDefault(com.pentolrebus.kasir.util.LogTag.APP),m)} else LocalPosRepository(secure){t,m->diagnostics.log(com.pentolrebus.kasir.util.LogTag.APP,m)}
  } catch(e:Exception){ diagnostics.log(com.pentolrebus.kasir.util.LogTag.ERROR,"Firebase unavailable; local development repository active"); LocalPosRepository(secure){t,m->diagnostics.log(com.pentolrebus.kasir.util.LogTag.APP,m)} }
 }
}
