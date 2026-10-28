package com.example.freshtrack.core.data.repository

import android.content.ContentValues
import com.example.freshtrack.core.database.FoodDatabase
import com.example.freshtrack.core.model.FoodItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FoodRepository(private val database: FoodDatabase) {
    private val _items = MutableStateFlow(loadItems())
    val items: StateFlow<List<FoodItem>> = _items.asStateFlow()

    fun add(item: FoodItem) {
        database.writableDatabase.insert("food_items", null, item.toValues())
        refresh()
    }

    fun delete(id: Long) {
        database.writableDatabase.delete("food_items", "id = ?", arrayOf(id.toString()))
        refresh()
    }

    private fun refresh() { _items.value = loadItems() }

    private fun loadItems(): List<FoodItem> {
        val result = mutableListOf<FoodItem>()
        database.readableDatabase.query(
            "food_items", null, null, null, null, null, "expiration_date ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += FoodItem(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                    expirationDate = cursor.getString(cursor.getColumnIndexOrThrow("expiration_date")),
                    location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode")),
                    estimatedValue = cursor.getDouble(cursor.getColumnIndexOrThrow("estimated_value")),
                )
            }
        }
        return result
    }

    private fun FoodItem.toValues() = ContentValues().apply {
        put("name", name)
        put("category", category)
        put("quantity", quantity)
        put("expiration_date", expirationDate)
        put("location", location)
        put("barcode", barcode)
        put("estimated_value", estimatedValue)
    }
}
