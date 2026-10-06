package com.sakukasir.pos.util

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OwnerMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        // MVP keeps notifications in the local repository. Backend FCM integration will deliver here.
    }
    override fun onNewToken(token: String) {
        // Integration point: send the refreshed token to the backend when Firebase Auth/RTDB is enabled.
    }
}
