package com.example.freshtrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.freshtrack.core.data.ReportMetrics
import com.example.freshtrack.core.model.FoodHistoryRecord
import java.util.Locale

/**
 * Design doc 2.7 "Reporting": basic summaries of usage, waste, and
 * estimated savings built from item history. Kept simple for v1 (totals +
 * outcome counts), structured so category breakdowns, a time range picker,
 * and trend charts can be added later without reworking this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(history: List<FoodHistoryRecord>, onBack: () -> Unit) {
    val totalSaved = ReportMetrics.totalSaved(history)
    val totalWasted = ReportMetrics.totalWasted(history)
    val donated = ReportMetrics.donatedCount(history)
    val consumed = ReportMetrics.consumedCount(history)
    val composted = ReportMetrics.compostedCount(history)
    val discarded = ReportMetrics.discardedCount(history)
    val co2 = ReportMetrics.co2AvoidedKg(history)
    val streak = ReportMetrics.zeroWasteStreakDays(history)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports 📊") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No history yet 🌱", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mark items as consumed, donated, composted, or discarded from your pantry, and your savings and waste reports will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ReportStatCard(
                            title = "Estimated Savings",
                            value = formatCurrency(totalSaved),
                            emoji = "💰",
                            bgColor = Color(0xFFE8F5E9),
                            valueColor = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f),
                        )
                        ReportStatCard(
                            title = "Estimated Waste",
                            value = formatCurrency(totalWasted),
                            emoji = "🗑️",
                            bgColor = Color(0xFFFFEBEE),
                            valueColor = Color(0xFFC62828),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ReportStatCard(
                            title = "CO2e Avoided",
                            value = "${"%.1f".format(co2)} kg",
                            emoji = "🌍",
                            bgColor = Color(0xFFE0F2F1),
                            valueColor = Color(0xFF00695C),
                            modifier = Modifier.weight(1f),
                        )
                        ReportStatCard(
                            title = "Zero-Waste Streak",
                            value = if (streak != null) "$streak day${if (streak == 1L) "" else "s"}" else "—",
                            emoji = "🔥",
                            bgColor = Color(0xFFFFF3E0),
                            valueColor = Color(0xFFE65100),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Text("Item Outcomes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutcomeRow("✅ Consumed", consumed)
                            OutcomeRow("🎁 Donated", donated)
                            OutcomeRow("🌱 Composted", composted)
                            OutcomeRow("🗑️ Discarded", discarded)
                        }
                    }
                }
                item {
                    Text(
                        text = "Estimates are based on the prices you enter when adding items, and are not exact financial records.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportStatCard(
    title: String,
    value: String,
    emoji: String,
    bgColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text(emoji, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
private fun OutcomeRow(label: String, count: Int) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("$count", fontWeight = FontWeight.Bold)
    }
}

private fun formatCurrency(value: Double): String = "$%.2f".format(Locale.US, value)
