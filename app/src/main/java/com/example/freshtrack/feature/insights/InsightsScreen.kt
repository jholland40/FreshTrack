package com.example.freshtrack.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.freshtrack.core.model.FoodItem
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Feature boundary for Member 4: Pantry statistics & insights screen.
 */
@Composable
fun InsightsScreen(
    foods: List<FoodItem>,
) {
    val expiringSoon = foods.count { item ->
        val days = daysUntil(item.expirationDate)
        (days != null) && (days >= 0) && (days <= 3)
    }
    val totalValue = foods.sumOf { it.estimatedValue * it.quantity }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Pantry summary",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        SummaryCard(
            title = "Items being tracked",
            value = foods.sumOf { it.quantity }.toString(),
            icon = Icons.Default.Inventory2,
        )
        SummaryCard(
            title = "Expiring within 3 days",
            value = expiringSoon.toString(),
            icon = Icons.Default.Schedule,
        )
        SummaryCard(
            title = "Estimated pantry value",
            value = "$%.2f".format(totalValue),
            icon = Icons.Default.Savings,
        )
        Text(
            text = "Waste and savings history will be added after the inventory workflow is tested.",
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title)
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun daysUntil(date: String): Long? = try {
    ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date))
} catch (_: DateTimeParseException) {
    null
}
