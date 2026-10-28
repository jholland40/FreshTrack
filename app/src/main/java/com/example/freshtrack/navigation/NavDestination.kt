package com.example.freshtrack.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(
    val label: String,
    val icon: ImageVector,
) {
    Pantry("Pantry", Icons.Default.Kitchen),
    Add("Add", Icons.Default.AddCircle),
    Scan("Scan", Icons.Default.QrCodeScanner),
    Insights("Insights", Icons.Default.BarChart),
    Settings("Settings", Icons.Default.Settings),
}
