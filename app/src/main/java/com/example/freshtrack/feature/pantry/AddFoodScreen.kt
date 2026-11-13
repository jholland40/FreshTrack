package com.example.freshtrack.feature.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.freshtrack.core.data.network.OpenFoodFactsService
import com.example.freshtrack.core.data.network.PluProLookup
import com.example.freshtrack.core.model.FoodItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Feature boundary for Member 4: Food addition form UI and validation state.
 */
@Composable
fun AddFoodScreen(
    onSave: (FoodItem) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var barcode by rememberSaveable { mutableStateOf("") }
    var pluCode by rememberSaveable { mutableStateOf("") }
    var lookupMessage by rememberSaveable { mutableStateOf<String?>(null) }

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

        // --- Packaged goods: barcode lookup via Open Food Facts ---
        item {
            Text(
                text = "Packaged item? Look it up by barcode",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    lookupMessage = "Looking up barcode..."
                    coroutineScope.launch {
                        val result = OpenFoodFactsService.lookup(barcode.trim())
                        if (result != null) {
                            name = result.name
                            category = result.category ?: category
                            lookupMessage = "Found: ${result.name}"
                        } else {
                            lookupMessage = "No match found, enter details manually."
                        }
                    }
                }) {
                    Text("Look up")
                }
            }
        }

        // --- Loose produce: PLU sticker code lookup ---
        item {
            Text(
                text = "Loose produce? Look it up by PLU sticker code",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = pluCode,
                    onValueChange = { pluCode = it },
                    label = { Text("PLU code") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    val result = PluProLookup.lookup(context, pluCode.trim())
                    if (result != null) {
                        name = result.name
                        category = result.category ?: category
                        lookupMessage = "Found: ${result.name}"
                    } else {
                        lookupMessage = "No match found, enter details manually."
                    }
                }) {
                    Text("Look up")
                }
            }
        }
        if (lookupMessage != null) {
            item {
                Text(
                    text = lookupMessage!!,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        item { HorizontalDivider() }

        // --- Manual entry / editable fields (always shown, pre-filled by a lookup above) ---
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
                                barcode = barcode.trim().ifBlank { pluCode.trim() },
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
