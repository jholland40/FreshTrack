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
    // Remembers every uid we've successfully confirmed HAS a saved name
    // during this process's lifetime. A transient Firestore read hiccup -
    // more likely when the device is under heavy load, e.g. mid-recording
    // with the camera and screen capture both competing for CPU/network -
    // must never flip an already-confirmed user back to "no name saved
    // yet": that is exactly what sends someone back through the one-time
    // name screen and makes the app look like it logged them out
    // mid-session. Once we've genuinely seen a name for a uid, we keep it
    // for the rest of this process, with no further Firestore reads needed.
    private val confirmedProfiles = mutableMapOf<String, UserProfile>()

    /** Returns the saved profile for this uid, or null if no name has been set yet. */
    suspend fun getProfile(uid: String): UserProfile? {
        confirmedProfiles[uid]?.let { return it }

        val snapshot = db.collection("users").document(uid).get().await()
        val name = snapshot.getString("displayName")?.takeIf { it.isNotBlank() } ?: return null
        val profile = UserProfile(uid = uid, displayName = name)
        confirmedProfiles[uid] = profile
        return profile
    }

    /** One-time write: called the first time a given uid is seen with no saved name. */
    suspend fun saveDisplayName(uid: String, name: String) {
        val data = hashMapOf(
            "uid" to uid,
            "displayName" to name,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        db.collection("users").document(uid).set(data, SetOptions.merge()).await()
        confirmedProfiles[uid] = UserProfile(uid = uid, displayName = name)
    }
}
