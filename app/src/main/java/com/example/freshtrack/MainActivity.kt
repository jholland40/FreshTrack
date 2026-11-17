package com.example.freshtrack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.freshtrack.core.data.repository.FoodRepository
import com.example.freshtrack.ui.FreshTrackDashboard
import com.example.freshtrack.ui.theme.FreshTrackTheme
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "FreshTrackAuth"

class MainActivity : ComponentActivity() {

    companion object {
        // Across Activity re-creations within the same app process (a
        // config change, or Android recreating the Activity under memory
        // pressure - more likely while screen recording is also competing
        // hard for CPU/RAM with the camera and ML Kit), make sure we only
        // ever ask Firebase to sign in anonymously ONCE per process.
        // Calling signInAnonymously() a second time while a real session
        // just hasn't settled yet mints a brand-new, empty identity -
        // which is what was making the pantry look like it "logged out"
        // mid-session.
        @Volatile
        private var signInRequested = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()

        // AuthStateListener (rather than a one-off synchronous currentUser
        // check) is the reliable way to know the real sign-in state - and
        // the guard above makes sure it can only ever trigger one
        // signInAnonymously() call for the lifetime of this process.
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d(TAG, "Auth state: signed in as uid=${user.uid}")
                return@addAuthStateListener
            }
            if (signInRequested) {
                Log.d(TAG, "Auth state: no user, but sign-in was already requested this process - not retrying")
                return@addAuthStateListener
            }
            signInRequested = true
            firebaseAuth.signInAnonymously()
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Anonymous sign-in succeeded: uid=${result.user?.uid}")
                }
                .addOnFailureListener { e ->
                    signInRequested = false
                    Log.e(TAG, "Anonymous sign-in FAILED - check network/Firebase console settings", e)
                }
        }

        val repository = FoodRepository()
        setContent {
            FreshTrackTheme {
                FreshTrackDashboard(repository)
            }
        }
    }
}
