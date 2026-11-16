package com.example.freshtrack.core.data.repository

import com.example.freshtrack.core.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Maps each Firebase Anonymous Auth uid to a one-time, human-chosen display
 * name, stored in a top-level "users" collection (document id = uid). This
 * is what lets us tell real testers/sessions apart on the Firebase console
 * and in Reports without switching to full email/password accounts.
 *
 * Nothing about sign-in itself changes: Firebase still mints one anonymous
 * uid per install and caches it locally. This just attaches a readable name
 * to that uid the first time we see it.
 */
class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    /** Returns the saved profile for this uid, or null if no name has been set yet. */
    suspend fun getProfile(uid: String): UserProfile? {
        val snapshot = db.collection("users").document(uid).get().await()
        val name = snapshot.getString("displayName")?.takeIf { it.isNotBlank() } ?: return null
        return UserProfile(uid = uid, displayName = name)
    }

    /** One-time write: called the first time a given uid is seen with no saved name. */
    suspend fun saveDisplayName(uid: String, name: String) {
        val data = hashMapOf(
            "uid" to uid,
            "displayName" to name,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        db.collection("users").document(uid).set(data, SetOptions.merge()).await()
    }
}
