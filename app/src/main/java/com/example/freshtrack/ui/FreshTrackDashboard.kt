package com.example.freshtrack.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Navigation Screens
sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.Kitchen)
    object Scanner : Screen("scanner", "Scan", Icons.Default.QrCodeScanner)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

// Filter enum for summary cards
enum class ItemFilter { ALL, EXPIRING_SOON }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshTrackDashboard() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isLoggedIn by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(ItemFilter.ALL) }

    // Initial Inventory Stash with urgent items
    val inventoryList = remember {
        mutableStateListOf(
            FoodItem(name = "Whole Milk", category = "Dairy", expiryDate = "Sep 21", quantity = 1, isExpiringSoon = true),
            FoodItem(name = "Doritos Nacho Cheese", category = "Snacks", expiryDate = "Nov 12", quantity = 1, isExpiringSoon = false),
            FoodItem(name = "Dr Pepper 12oz", category = "Beverages", expiryDate = "Dec 05", quantity = 6, isExpiringSoon = false),
            FoodItem(name = "Organic Spinach", category = "Produce", expiryDate = "Sep 23", quantity = 1, isExpiringSoon = false)
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }

    // Mint Green Ombré Palette & Dynamic Theme Colors
    val mintDark = Color(0xFF1B5E20)
    val mintPrimary = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
    val appBackground = if (isDarkMode) Color(0xFF121212) else Color(0xFFF1F8F5)
    val topBarContainer = if (isDarkMode) Color(0xFF1B5E20) else Color(0xFF1B5E20)
    val cardBackground = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF0D3B12)

    val ombréBrush = Brush.verticalGradient(
        colors = if (isDarkMode) {
            listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
        } else {
            listOf(Color(0xFF66BB6A), Color(0xFFA8E6CF))
        }
    )

    if (!isLoggedIn) {
        WelcomeScreen(
            onGetStarted = { isLoggedIn = true },
            ombréBrush = ombréBrush,
            isDarkMode = isDarkMode
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🥤", fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "FreshTrack ✦",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 19.sp
                            )
                        }
                    },
                    actions = {
                        // Light / Dark Mode Icon Toggle
                        IconButton(
                            onClick = { isDarkMode = !isDarkMode },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(if (isDarkMode) "🌙" else "☀️", fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // "+ Add Item" Action Button
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF176)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "➕ Add Item",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF33691E),
                                fontSize = 12.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                    tonalElevation = 10.dp
                ) {
                    val items = listOf(Screen.Home, Screen.Inventory, Screen.Scanner, Screen.Profile)
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title, modifier = Modifier.size(22.dp)) },
                            label = { Text(screen.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = mintPrimary,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = mintPrimary,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = if (isDarkMode) Color(0xFF2E7D32) else Color(0xFFE8F5E9)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBackground)
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        inventoryList = inventoryList,
                        ombréBrush = ombréBrush,
                        activeFilter = activeFilter,
                        onSelectFilter = { filter ->
                            activeFilter = filter
                        },
                        isDarkMode = isDarkMode,
                        cardBackground = cardBackground,
                        textColor = textColor
                    )
                    Screen.Inventory -> InventoryScreen(
                        inventoryList = inventoryList,
                        activeFilter = activeFilter,
                        onClearFilter = { activeFilter = ItemFilter.ALL },
                        isDarkMode = isDarkMode,
                        cardBackground = cardBackground,
                        textColor = textColor
                    )
                    Screen.Scanner -> ScannerScreen(isDarkMode)
                    Screen.Profile -> ProfileScreen(onLogout = { isLoggedIn = false }, isDarkMode = isDarkMode)
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
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, ombréBrush: Brush, isDarkMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ombréBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text("🍿", fontSize = 48.sp) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("FreshTrack ✨", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = if (isDarkMode) Color.White else Color(0xFF1B5E20))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Track your snacks, sodas, and groceries easily!", style = MaterialTheme.typography.bodyLarge, color = if (isDarkMode) Color(0xFFC8E6C9) else Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF176)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Sign Up / Log In 🚀", color = Color(0xFF33691E), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun HomeScreen(
    inventoryList: List<FoodItem>,
    ombréBrush: Brush,
    activeFilter: ItemFilter,
    onSelectFilter: (ItemFilter) -> Unit,
    isDarkMode: Boolean,
    cardBackground: Color,
    textColor: Color
) {
    val expiringCount = inventoryList.count { it.isExpiringSoon }
    val displayList = when (activeFilter) {
        ItemFilter.EXPIRING_SOON -> inventoryList.filter { it.isExpiringSoon }
        ItemFilter.ALL -> inventoryList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Mint Green Ombré Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ombréBrush)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Welcome back! ✨",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have ${inventoryList.size} items in your stash. Tapping summary cards below filters your list!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) Color(0xFFE8F5E9) else Color(0xFF2E7D32)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Pantry Summary 📊",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // INTERACTIVE SUMMARY CARDS GRID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Items Card
            ClickableSummaryCard(
                title = "Total Items",
                count = "${inventoryList.size}",
                emoji = "🛒",
                bgColor = if (isDarkMode) Color(0xFF2C3E35) else Color(0xFFE0F2F1),
                borderColor = if (activeFilter == ItemFilter.ALL) Color(0xFF4CAF50) else Color.Transparent,
                isDarkMode = isDarkMode,
                onClick = { onSelectFilter(ItemFilter.ALL) },
                modifier = Modifier.weight(1f)
            )

            // Expiring Soon Card (Red Urgent Palette)
            ClickableSummaryCard(
                title = "Expiring Soon",
                count = "$expiringCount",
                emoji = "⚠️",
                bgColor = if (isDarkMode) Color(0xFF3E2723) else Color(0xFFFFEBEE),
                borderColor = if (activeFilter == ItemFilter.EXPIRING_SOON) Color(0xFFE53935) else Color.Transparent,
                isDarkMode = isDarkMode,
                isUrgent = true,
                onClick = { onSelectFilter(ItemFilter.EXPIRING_SOON) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Title & Filter Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeFilter == ItemFilter.EXPIRING_SOON) "⚠️ Items Expiring Soon" else "🍿 Active Stash",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )

            if (activeFilter != ItemFilter.ALL) {
                TextButton(onClick = { onSelectFilter(ItemFilter.ALL) }) {
                    Text("Clear Filter ✕", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Inventory Stash List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(displayList) { item ->
                InventoryCard(item = item, isDarkMode = isDarkMode, cardBackground = cardBackground)
            }
        }
    }
}

@Composable
fun ClickableSummaryCard(
    title: String,
    count: String,
    emoji: String,
    bgColor: Color,
    borderColor: Color,
    isDarkMode: Boolean,
    isUrgent: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUrgent) Color(0xFFC62828) else if (isDarkMode) Color.LightGray else Color.DarkGray
                )
                Text(text = emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isUrgent) Color(0xFFD32F2F) else if (isDarkMode) Color.White else Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tap to view →",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUrgent) Color(0xFFC62828) else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InventoryScreen(
    inventoryList: List<FoodItem>,
    activeFilter: ItemFilter,
    onClearFilter: () -> Unit,
    isDarkMode: Boolean,
    cardBackground: Color,
    textColor: Color
) {
    val filteredList = when (activeFilter) {
        ItemFilter.EXPIRING_SOON -> inventoryList.filter { it.isExpiringSoon }
        ItemFilter.ALL -> inventoryList
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeFilter == ItemFilter.EXPIRING_SOON) "⚠️ Expiring Soon" else "Active Stash 🍿",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            if (activeFilter != ItemFilter.ALL) {
                TextButton(onClick = onClearFilter) {
                    Text("Show All", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(filteredList) { item ->
                InventoryCard(item, isDarkMode, cardBackground)
            }
        }
    }
}

@Composable
fun InventoryCard(item: FoodItem, isDarkMode: Boolean, cardBackground: Color) {
    val categoryEmoji = when (item.category.lowercase()) {
        "snacks" -> "🍿"
        "beverages" -> "🥤"
        "sweets" -> "🍫"
        "produce" -> "🥬"
        "dairy" -> "🥛"
        "meat" -> "🥩"
        "bakery" -> "🍞"
        else -> "🍎"
    }

    val badgeBg = if (item.isExpiringSoon) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
    val badgeTextColor = if (item.isExpiringSoon) Color(0xFFC62828) else Color(0xFF2E7D32)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        Text(categoryEmoji, fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
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
                color = badgeBg,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (item.isExpiringSoon) "⚠️ Exp: ${item.expiryDate}" else "Exp: ${item.expiryDate}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeTextColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ScannerScreen(isDarkMode: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📸", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Barcode Scanner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit, isDarkMode: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = Color(0xFFE8F5E9), modifier = Modifier.size(90.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("🤠", fontSize = 48.sp) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Hawks",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkMode) Color.White else Color.Black
        )
        Text(text = "freshtrack.user@example.com", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Log Out 👋", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}