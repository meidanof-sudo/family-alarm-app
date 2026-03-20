package com.familyalarm.service

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Triggers the alarm for a specific family member by writing an alarm document
 * to Firestore. A Cloud Function (see /cloud-functions) watches for these
 * writes and sends a high-priority FCM message to the targeted device.
 */
object AlarmTrigger {

    suspend fun sendAlarm(
        familyCode: String,
        senderName: String,
        senderUid: String,
        targetUid: String,
        targetName: String
    ) {
        val db = FirebaseFirestore.getInstance()

        val alarmData = hashMapOf(
            "familyCode" to familyCode,
            "senderName" to senderName,
            "senderUid" to senderUid,
            "targetUid" to targetUid,
            "targetName" to targetName,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("alarms").add(alarmData).await()
    }
}
