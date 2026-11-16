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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            // Logs a clear success/failure line tagged "FreshTrackAuth" so a
            // Logcat search for that tag always shows what happened here -
            // instead of needing to guess whether sign-in even ran.
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Anonymous sign-in succeeded: uid=${result.user?.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Anonymous sign-in FAILED - check network/Firebase console settings", e)
                }
        } else {
            Log.d(TAG, "Already signed in: uid=${auth.currentUser?.uid}")
        }

        val repository = FoodRepository()
        setContent {
            FreshTrackTheme {
                FreshTrackDashboard(repository)
            }
        }
    }
}
