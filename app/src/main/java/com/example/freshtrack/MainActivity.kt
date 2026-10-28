package com.example.freshtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.freshtrack.core.database.FoodDatabase
import com.example.freshtrack.core.data.repository.FoodRepository
import com.example.freshtrack.ui.FreshTrackApp
import com.example.freshtrack.ui.FreshTrackDashboard
import com.example.freshtrack.ui.theme.FreshTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = FoodDatabase(applicationContext)
        val repository = FoodRepository(database)

        setContent {
            FreshTrackTheme {
                FreshTrackApp(repository)
                FreshTrackDashboard()
            }
        }
    }
}
