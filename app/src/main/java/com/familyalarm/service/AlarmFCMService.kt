package com.familyalarm.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming FCM messages. When a high-priority data message
 * arrives with type "family_alarm", it starts the AlarmSoundService
 * which plays the alarm bypassing DND.
 */
class AlarmFCMService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        if (data["type"] == "family_alarm") {
            val senderName = data["senderName"] ?: "Family Member"
            AlarmSoundService.start(this, senderName)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token refresh is handled when the app opens and updates Firestore
    }
}
