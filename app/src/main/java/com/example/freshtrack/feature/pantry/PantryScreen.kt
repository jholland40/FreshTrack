package com.example.freshtrack.feature.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.freshtrack.core.model.FoodItem
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Feature boundary for Member 4: Pantry UI, item list, card layout, and accessibility tags.
 */
@Composable
fun PantryScreen(
    foods: List<FoodItem>,
    onDelete: (Long) -> Unit,
) {
    if (foods.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your pantry is empty",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Tap Add to start tracking food and expiration dates.",
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "${foods.size} items",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(foods, key = { it.id }) { food ->
                FoodCard(food = food, onDelete = onDelete)
            }
        }
    }
}

@Composable
fun FoodCard(
    food: FoodItem,
    onDelete: (Long) -> Unit,
) {
    val days = daysUntil(food.expirationDate)
    val status = when {
        days == null -> "Date needs review"
        days < 0 -> "Expired ${-days} day(s) ago"
        days == 0L -> "Expires today"
        else -> "Expires in $days day(s)"
    }
    val statusColor = if ((days != null) && (days <= 3)) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${food.category} • ${food.location} • Qty ${food.quantity}",
                )
                Text(
                    text = status,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(onClick = { onDelete(food.id) }) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete ${food.name}",
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
