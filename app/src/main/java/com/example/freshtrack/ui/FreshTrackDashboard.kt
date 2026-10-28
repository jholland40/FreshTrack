package com.example.freshtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshTrackDashboard() {
    val inventoryList = remember {
        mutableStateListOf(
            FoodItem(name = "Organic Spinach", category = "Produce", expiryDate = "Sep 23", quantity = 1),
            FoodItem(name = "Lettuce", category = "Produce", expiryDate = "Sep 23", quantity = 1),
            FoodItem(name = "Whole Milk", category = "Dairy", expiryDate = "Sep 21", quantity = 2)
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }

    // Forest Green Theme Palette
    val forestGreen = Color(0xFF1B5E20)
    val lightGreenAccent = Color(0xFFE8F5E9)
    val surfaceColor = Color(0xFFF4F6F4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Kitchen,
                                contentDescription = null,
                                tint = forestGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "FreshTrack",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = forestGreen
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White) },
                text = { Text("Add Item", fontWeight = FontWeight.Bold, color = Color.White) },
                containerColor = forestGreen,
                shape = RoundedCornerShape(28.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = lightGreenAccent)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pantry Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = forestGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${inventoryList.size} items tracked to prevent waste",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Active Inventory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(inventoryList) { item ->
                        InventoryCard(item)
                    }
                }
            }

            if (showAddDialog) {
                AddItemModal(
                    onDismiss = { showAddDialog = false },
                    onAddItem = { newItem ->
                        inventoryList.add(0, newItem)
                    }
                )
            }
        }
    }
}

@Composable
fun InventoryCard(item: FoodItem) {
    val categoryEmoji = when (item.category.lowercase()) {
        "produce" -> "🥬"
        "dairy" -> "🥛"
        "meat" -> "🥩"
        "bakery" -> "🍞"
        else -> "🍎"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = categoryEmoji, fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.category} • Qty: ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Surface(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Exp: ${item.expiryDate}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}