package com.example.freshtrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.freshtrack.core.data.network.OpenFoodFactsService
import com.example.freshtrack.core.data.network.PluProLookup
import com.example.freshtrack.core.model.FoodItem as RealFoodItem
import com.example.freshtrack.core.model.ProductInfo
import kotlinx.coroutines.launch
import java.time.LocalDate


/**
 * @param prefill A product handed off from the Scan tab after a successful
 * barcode read. Pre-fills name/category/barcode but every field stays
 * editable - matches the same pre-fill behavior AddFoodScreen uses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemModal(
    prefill: ProductInfo? = null,
    onDismiss: () -> Unit,
    onAddItem: (RealFoodItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var barcode by remember { mutableStateOf(prefill?.barcode.orEmpty()) }
    var pluCode by remember { mutableStateOf("") }
    var lookupMessage by remember {
        mutableStateOf(
            when {
                prefill == null -> null
                prefill.name.isBlank() -> "Scanned ${prefill.barcode}. No match found, enter details manually."
                else -> "Scanned: ${prefill.name}"
            },
        )
    }

    var name by remember { mutableStateOf(prefill?.name.orEmpty()) }
    var category by remember { mutableStateOf(prefill?.category ?: "Snacks") }
    var expiryDate by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var quantityStr by remember { mutableStateOf("1") }

    val categories = listOf("Snacks", "Beverages", "Produce", "Dairy", "Sweets", "Bakery", "Meat", "Other")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Add Item to Stash 🛒",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1B5E20),
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // --- Packaged goods: barcode lookup via Open Food Facts ---
                Text(
                    text = "Packaged item? Look it up by barcode",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF2E7D32),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
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
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Look up", color = Color.White)
                    }
                }

                // --- Loose produce: PLU sticker code lookup ---
                Text(
                    text = "Loose produce? Look it up by PLU code",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF2E7D32),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pluCode,
                        onValueChange = { pluCode = it },
                        label = { Text("PLU code") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            val result = PluProLookup.lookup(context, pluCode.trim())
                            if (result != null) {
                                name = result.name
                                category = result.category ?: category
                                lookupMessage = "Found: ${result.name}"
                            } else {
                                lookupMessage = "No match found, enter details manually."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Look up", color = Color.White)
                    }
                }
                if (lookupMessage != null) {
                    Text(text = lookupMessage!!, color = Color(0xFF2E7D32), fontSize = 12.sp)
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Doritos, Dr Pepper)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiration (YYYY-MM-DD)") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Qty") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val qty = quantityStr.toIntOrNull() ?: 1
                        onAddItem(
                            RealFoodItem(
                                name = name.trim(),
                                category = category.trim(),
                                quantity = qty,
                                expirationDate = expiryDate.trim(),
                                location = "Pantry",
                                barcode = barcode.trim().ifBlank { pluCode.trim() },
                                estimatedValue = 0.0,
                            )
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Item ✨", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    )
}
