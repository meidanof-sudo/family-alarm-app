package com.familyalarm.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class FamilyMember(
    val uid: String = "",
    val name: String = "",
    val fcmToken: String = ""
)

data class Family(
    val code: String = "",
    val members: List<FamilyMember> = emptyList()
)

class FamilyRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private suspend fun ensureAnonymousAuth(): String {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        return user?.uid ?: throw Exception("Authentication failed")
    }

    private suspend fun getFcmToken(): String {
        return FirebaseMessaging.getInstance().token.await()
    }

    suspend fun createFamily(memberName: String): String {
        val uid = ensureAnonymousAuth()
        val token = getFcmToken()
        val code = generateFamilyCode()

        val member = hashMapOf(
            "uid" to uid,
            "name" to memberName,
            "fcmToken" to token
        )

        db.collection("families").document(code).set(
            hashMapOf(
                "code" to code,
                "members" to listOf(member),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        return code
    }

    suspend fun joinFamily(code: String, memberName: String) {
        val uid = ensureAnonymousAuth()
        val token = getFcmToken()

        val member = hashMapOf(
            "uid" to uid,
            "name" to memberName,
            "fcmToken" to token
        )

        val docRef = db.collection("families").document(code)
        val doc = docRef.get().await()

        if (!doc.exists()) {
            throw Exception("Family not found. Check the code and try again.")
        }

        docRef.update("members", FieldValue.arrayUnion(member)).await()
    }

    suspend fun leaveFamily(code: String) {
        val uid = ensureAnonymousAuth()
        val docRef = db.collection("families").document(code)
        val doc = docRef.get().await()

        if (!doc.exists()) return

        @Suppress("UNCHECKED_CAST")
        val members = doc.get("members") as? List<Map<String, Any>> ?: return
        val myEntry = members.find { it["uid"] == uid } ?: return

        docRef.update("members", FieldValue.arrayRemove(myEntry)).await()
    }

    suspend fun getFamily(code: String): Family {
        val doc = db.collection("families").document(code).get().await()
        if (!doc.exists()) throw Exception("Family not found")

        @Suppress("UNCHECKED_CAST")
        val membersData = doc.get("members") as? List<Map<String, Any>> ?: emptyList()
        val members = membersData.map { m ->
            FamilyMember(
                uid = m["uid"] as? String ?: "",
                name = m["name"] as? String ?: "",
                fcmToken = m["fcmToken"] as? String ?: ""
            )
        }
        return Family(code = code, members = members)
    }

    suspend fun getFcmTokensExceptSelf(code: String): List<Pair<String, String>> {
        val uid = ensureAnonymousAuth()
        val family = getFamily(code)
        return family.members
            .filter { it.uid != uid && it.fcmToken.isNotBlank() }
            .map { it.fcmToken to it.name }
    }

    suspend fun updateMyToken(code: String, memberName: String) {
        val uid = ensureAnonymousAuth()
        val token = getFcmToken()
        val docRef = db.collection("families").document(code)
        val doc = docRef.get().await()
        if (!doc.exists()) return

        @Suppress("UNCHECKED_CAST")
        val members = doc.get("members") as? List<Map<String, Any>> ?: return
        val oldEntry = members.find { it["uid"] == uid } ?: return

        val newEntry = hashMapOf(
            "uid" to uid,
            "name" to memberName,
            "fcmToken" to token
        )

        docRef.update("members", FieldValue.arrayRemove(oldEntry)).await()
        docRef.update("members", FieldValue.arrayUnion(newEntry)).await()
    }

    fun getCurrentUid(): String? = auth.currentUser?.uid

    private fun generateFamilyCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
