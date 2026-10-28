package com.example.freshtrack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.freshtrack.core.data.repository.FoodRepository
import com.example.freshtrack.feature.insights.InsightsScreen
import com.example.freshtrack.feature.pantry.AddFoodScreen
import com.example.freshtrack.feature.pantry.PantryScreen
import com.example.freshtrack.feature.scanner.ScannerScreen
import com.example.freshtrack.feature.settings.SettingsScreen
import com.example.freshtrack.navigation.NavDestination

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FreshTrackApp(repository: FoodRepository) {
    val viewModel = remember(repository) { FoodViewModel(repository) }
    val foods by viewModel.items.collectAsStateWithLifecycle()
    var currentDestination by rememberSaveable { mutableStateOf(NavDestination.Pantry) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FreshTrack • ${currentDestination.label}") },
            )
        },
        bottomBar = {
            NavigationBar {
                NavDestination.entries.forEach { destination ->
                    NavItem(
                        selected = currentDestination == destination,
                        label = destination.label,
                        icon = destination.icon,
                    ) { currentDestination = destination }
                }
            }
        },
        floatingActionButton = {
            if (currentDestination == NavDestination.Pantry) {
                FloatingActionButton(onClick = { currentDestination = NavDestination.Add }) {
                    Icon(Icons.Default.Add, contentDescription = "Add food")
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (currentDestination) {
                NavDestination.Pantry -> PantryScreen(
                    foods = foods,
                    onDelete = viewModel::deleteFood,
                )
                NavDestination.Add -> AddFoodScreen { item ->
                    viewModel.addFood(item)
                    currentDestination = NavDestination.Pantry
                }
                NavDestination.Scan -> ScannerScreen()
                NavDestination.Insights -> InsightsScreen(foods = foods)
                NavDestination.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
    )
}
