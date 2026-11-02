package com.example.freshtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.freshtrack.core.data.repository.FoodRepository
import com.example.freshtrack.ui.FreshTrackApp
import com.example.freshtrack.ui.theme.FreshTrackTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }

        val repository = FoodRepository()
        setContent {
            FreshTrackTheme {
                FreshTrackApp(repository)
            }
        }
    }
}