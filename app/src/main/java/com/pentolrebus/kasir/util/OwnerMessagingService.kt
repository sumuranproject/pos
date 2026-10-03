package com.pentolrebus.kasir.util
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
class OwnerMessagingService:FirebaseMessagingService(){override fun onMessageReceived(message:RemoteMessage){/* Owner notification rendering can be delegated to NotificationManager in production. */}override fun onNewToken(token:String){/* Repository can persist token under users/{uid}/deviceTokens after authenticated Owner session. */}}
