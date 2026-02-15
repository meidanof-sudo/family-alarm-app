package com.familyalarm.service

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Triggers the alarm for all family members by writing an alarm document
 * to Firestore. A Cloud Function (see /cloud-functions) watches for these
 * writes and sends high-priority FCM messages to each family member's device.
 */
object AlarmTrigger {

    suspend fun sendAlarm(familyCode: String, senderName: String, senderUid: String) {
        val db = FirebaseFirestore.getInstance()

        val alarmData = hashMapOf(
            "familyCode" to familyCode,
            "senderName" to senderName,
            "senderUid" to senderUid,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("alarms").add(alarmData).await()
    }
}
