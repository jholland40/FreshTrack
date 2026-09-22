package com.example.freshtrack.feature.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.freshtrack.core.model.FoodItem
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Feature boundary for Member 4: Food addition form UI and validation state.
 */
@Composable
fun AddFoodScreen(
    onSave: (FoodItem) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Other") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var expiration by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var location by rememberSaveable { mutableStateOf("Refrigerator") }
    var value by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Add a food item",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item { Field(name, { name = it }, "Food name") }
        item { Field(category, { category = it }, "Category") }
        item { Field(quantity, { quantity = it }, "Quantity") }
        item { Field(expiration, { expiration = it }, "Expiration date (YYYY-MM-DD)") }
        item { Field(location, { location = it }, "Storage location") }
        item { Field(value, { value = it }, "Estimated value (optional)") }
        if (error != null) {
            item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(
                onClick = {
                    val parsedDate = try {
                        LocalDate.parse(expiration)
                    } catch (_: DateTimeParseException) {
                        null
                    }
                    val parsedQuantity = quantity.toIntOrNull()
                    if (name.isBlank() || (parsedDate == null) || (parsedQuantity == null) || (parsedQuantity < 1)) {
                        error = "Enter a name, valid date, and quantity of at least 1."
                    } else {
                        onSave(
                            FoodItem(
                                name = name.trim(),
                                category = category.trim(),
                                quantity = parsedQuantity,
                                expirationDate = parsedDate.toString(),
                                location = location.trim(),
                                estimatedValue = value.toDoubleOrNull() ?: 0.0,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save item")
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
