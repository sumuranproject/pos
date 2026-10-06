package com.pentolrebus.kasir.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object RepositoryProvider {
 fun create(context:Context):PosRepository {
  val secure=SecureLocalStore(context)
  return try {
   val app=FirebaseApp.initializeApp(context)
   if(app!=null) FirebasePosRepository(FirebaseAuth.getInstance(app),FirebaseDatabase.getInstance(app).reference,secure){_,_->} else LocalPosRepository(secure){_,_->}
  } catch(e:Exception){ LocalPosRepository(secure){_,_->} }
 }
}
