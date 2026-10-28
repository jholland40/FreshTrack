package com.example.freshtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.freshtrack.ui.FreshTrackDashboard
import com.example.freshtrack.ui.theme.FreshTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreshTrackTheme {
                FreshTrackDashboard()
            }
        }
    }
}