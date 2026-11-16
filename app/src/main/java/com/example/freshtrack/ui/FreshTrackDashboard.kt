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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.freshtrack.core.data.ReportMetrics
import com.example.freshtrack.core.data.repository.FoodRepository
import com.example.freshtrack.core.data.repository.UserRepository
import com.example.freshtrack.core.model.FoodHistoryRecord
import com.example.freshtrack.core.model.FoodStatus
import com.example.freshtrack.core.model.ProductInfo
import com.example.freshtrack.core.model.FoodItem as RealFoodItem
import com.example.freshtrack.feature.scanner.ScannerScreen as CameraScannerScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// Tracks whether we know this uid's display name yet. Every anonymous
// Firebase uid gets a one-time name prompt (see NameEntryScreen below),
// written to a "users" collection, so sessions are traceable to a person on
// the Firebase console and in Reports instead of a bare uid string.
private sealed interface ProfileCheck {
    object Loading : ProfileCheck
    object NeedsName : ProfileCheck
    data class Ready(val displayName: String) : ProfileCheck
}

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
fun FreshTrackDashboard(repository: FoodRepository) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isLoggedIn by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(ItemFilter.ALL) }
    var showReports by remember { mutableStateOf(false) }

    // --- Identity: make sure this session's uid has a human name attached ---
    val userRepository = remember { UserRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var currentUid by remember { mutableStateOf(auth.currentUser?.uid) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { fbAuth -> currentUid = fbAuth.currentUser?.uid }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    var profileCheck by remember { mutableStateOf<ProfileCheck>(ProfileCheck.Loading) }
    LaunchedEffect(currentUid) {
        val uid = currentUid ?: return@LaunchedEffect
        profileCheck = userRepository.getProfile(uid)?.let { ProfileCheck.Ready(it.displayName) }
            ?: ProfileCheck.NeedsName
    }

    // Real data: same repository/ViewModel the rest of the app uses, so items
    // actually save to and load from Firestore instead of a fake in-memory list.
    val viewModel = remember(repository) { FoodViewModel(repository) }
    val foods by viewModel.items.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val inventoryList = remember(foods) { foods.map { it.toDashboardItem() } }

    // Resolving an item (not a plain delete) records why it left the pantry,
    // so Reports has real history to work from. Plain delete is reserved for
    // correcting a mistaken entry only.
    val onResolveItem: (String, FoodStatus) -> Unit = resolve@{ itemId, status ->
        val realItem = foods.find { it.id == itemId } ?: return@resolve
        viewModel.resolveFood(realItem, status)
    }
    val onDeleteMistake: (String) -> Unit = { itemId -> viewModel.deleteFood(itemId) }

    var showAddDialog by remember { mutableStateOf(false) }
    // A product handed off from the camera scanner, waiting to pre-fill the Add popup.
    var scannedProduct by remember { mutableStateOf<ProductInfo?>(null) }

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

    // Gate the whole dashboard behind knowing who this uid belongs to.
    // Loading: waiting on anonymous sign-in / the Firestore lookup.
    // NeedsName: signed in, but no "users" doc yet - ask once, then save it.
    val resolvedProfile = profileCheck
    when (resolvedProfile) {
        is ProfileCheck.Loading -> {
            LoadingScreen()
            return
        }
        is ProfileCheck.NeedsName -> {
            NameEntryScreen(
                onSubmit = { name ->
                    val uid = currentUid ?: return@NameEntryScreen
                    coroutineScope.launch {
                        userRepository.saveDisplayName(uid, name)
                        profileCheck = ProfileCheck.Ready(name)
                    }
                },
            )
            return
        }
        is ProfileCheck.Ready -> Unit
    }
    val displayName = (resolvedProfile as ProfileCheck.Ready).displayName

    if (showReports) {
        ReportsScreen(history = history, onBack = { showReports = false })
        return
    }

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
                        history = history,
                        ombréBrush = ombréBrush,
                        activeFilter = activeFilter,
                        onSelectFilter = { filter ->
                            activeFilter = filter
                        },
                        onOpenReports = { showReports = true },
                        isDarkMode = isDarkMode,
                        cardBackground = cardBackground,
                        textColor = textColor,
                        onResolve = onResolveItem,
                        onDeleteMistake = onDeleteMistake,
                    )
                    Screen.Inventory -> InventoryScreen(
                        inventoryList = inventoryList,
                        activeFilter = activeFilter,
                        onClearFilter = { activeFilter = ItemFilter.ALL },
                        isDarkMode = isDarkMode,
                        cardBackground = cardBackground,
                        textColor = textColor,
                        onResolve = onResolveItem,
                        onDeleteMistake = onDeleteMistake,
                    )
                    Screen.Scanner -> CameraScannerScreen(
                        onAddProduct = { product ->
                            scannedProduct = product
                            showAddDialog = true
                        },
                    )
                    Screen.Profile -> ProfileScreen(
                        onLogout = { isLoggedIn = false },
                        isDarkMode = isDarkMode,
                        onViewReports = { showReports = true },
                        displayName = displayName,
                        uid = currentUid ?: "",
                    )
                }

                if (showAddDialog) {
                    AddItemModal(
                        prefill = scannedProduct,
                        onDismiss = {
                            showAddDialog = false
                            scannedProduct = null
                        },
                        onAddItem = { newItem ->
                            // Saves to Firestore via the real repository; the snapshot
                            // listener above picks it up and the list updates itself.
                            viewModel.addFood(newItem)
                        }
                    )
                }
            }
        }
    }
}

// Converts a real, Firestore-backed FoodItem into the dashboard's own display
// shape: a friendly "Sep 21" style date, and an isExpiringSoon flag computed
// from the real expiration date instead of a manually-set checkbox.
private val dashboardDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

private fun RealFoodItem.toDashboardItem(): FoodItem {
    val parsedDate = try {
        LocalDate.parse(expirationDate)
    } catch (_: DateTimeParseException) {
        null
    }
    val daysLeft = parsedDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    return FoodItem(
        id = id,
        name = name,
        category = category,
        expiryDate = parsedDate?.format(dashboardDateFormatter) ?: expirationDate,
        quantity = quantity,
        isExpiringSoon = daysLeft != null && daysLeft <= 3,
    )
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF2E7D32))
    }
}

/**
 * One-time prompt shown the first time a given anonymous uid is seen with
 * no saved name. Every field stays simple on purpose - this isn't a real
 * account system, just a label so the uid means something to a human.
 */
@Composable
private fun NameEntryScreen(onSubmit: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val brush = Brush.verticalGradient(colors = listOf(Color(0xFF66BB6A), Color(0xFFA8E6CF)))

    Box(
        modifier = Modifier.fillMaxSize().background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Text("👋", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What should we call you?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1B5E20),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your Inventory & Reports are tied to your name. Enter it just once.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { if (name.isNotBlank()) onSubmit(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Let's go 🌱", color = Color.White, fontWeight = FontWeight.Bold)
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
    history: List<FoodHistoryRecord>,
    ombréBrush: Brush,
    activeFilter: ItemFilter,
    onSelectFilter: (ItemFilter) -> Unit,
    onOpenReports: () -> Unit,
    isDarkMode: Boolean,
    cardBackground: Color,
    textColor: Color,
    onResolve: (String, FoodStatus) -> Unit,
    onDeleteMistake: (String) -> Unit,
) {
    val expiringCount = inventoryList.count { it.isExpiringSoon }
    val displayList = when (activeFilter) {
        ItemFilter.EXPIRING_SOON -> inventoryList.filter { it.isExpiringSoon }
        ItemFilter.ALL -> inventoryList
    }
    val totalSaved = remember(history) { ReportMetrics.totalSaved(history) }
    val streak = remember(history) { ReportMetrics.zeroWasteStreakDays(history) }

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

        // INTERACTIVE SUMMARY CARDS GRID - 2x2 so the home page isn't lopsided
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClickableSummaryCard(
                title = "Est. Savings",
                count = "$%.0f".format(totalSaved),
                emoji = "💰",
                bgColor = if (isDarkMode) Color(0xFF1B3A2A) else Color(0xFFE8F5E9),
                borderColor = Color.Transparent,
                isDarkMode = isDarkMode,
                onClick = onOpenReports,
                modifier = Modifier.weight(1f)
            )
            ClickableSummaryCard(
                title = "Zero-Waste Streak",
                count = if (streak != null) "${streak}d" else "—",
                emoji = "🔥",
                bgColor = if (isDarkMode) Color(0xFF3E2E16) else Color(0xFFFFF3E0),
                borderColor = Color.Transparent,
                isDarkMode = isDarkMode,
                onClick = onOpenReports,
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
                InventoryCard(
                    item = item,
                    isDarkMode = isDarkMode,
                    cardBackground = cardBackground,
                    onResolve = onResolve,
                    onDeleteMistake = onDeleteMistake,
                )
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
    textColor: Color,
    onResolve: (String, FoodStatus) -> Unit,
    onDeleteMistake: (String) -> Unit,
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
                InventoryCard(
                    item = item,
                    isDarkMode = isDarkMode,
                    cardBackground = cardBackground,
                    onResolve = onResolve,
                    onDeleteMistake = onDeleteMistake,
                )
            }
        }
    }
}

@Composable
fun InventoryCard(
    item: FoodItem,
    isDarkMode: Boolean,
    cardBackground: Color,
    onResolve: (String, FoodStatus) -> Unit,
    onDeleteMistake: (String) -> Unit,
) {
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
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
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

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Remove ${item.name}",
                        tint = if (isDarkMode) Color.LightGray else Color.Gray,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("✅ Mark Consumed") },
                        onClick = { menuOpen = false; onResolve(item.id, FoodStatus.CONSUMED) },
                    )
                    DropdownMenuItem(
                        text = { Text("🎁 Mark Donated") },
                        onClick = { menuOpen = false; onResolve(item.id, FoodStatus.DONATED) },
                    )
                    DropdownMenuItem(
                        text = { Text("🌱 Mark Composted") },
                        onClick = { menuOpen = false; onResolve(item.id, FoodStatus.COMPOSTED) },
                    )
                    DropdownMenuItem(
                        text = { Text("🗑️ Mark Discarded") },
                        onClick = { menuOpen = false; onResolve(item.id, FoodStatus.DISCARDED) },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("✖️ Remove (mistake, no record)", color = Color.Gray) },
                        onClick = { menuOpen = false; onDeleteMistake(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    isDarkMode: Boolean,
    onViewReports: () -> Unit,
    displayName: String,
    uid: String,
) {
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
            text = displayName.ifBlank { "FreshTrack User" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkMode) Color.White else Color.Black
        )
        // Shown so testers/graders can match what's on screen to a row in the
        // Firebase console's Authentication tab or the "users" collection.
        Text(
            text = "Session: ${uid.take(8)}${if (uid.length > 8) "…" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onViewReports,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text("View Reports 📊", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text("Log Out 👋", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
