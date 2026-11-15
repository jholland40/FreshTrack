package com.example.freshtrack.ui

import androidx.lifecycle.ViewModel
import com.example.freshtrack.core.data.repository.FoodRepository
import com.example.freshtrack.core.model.FoodHistoryRecord
import com.example.freshtrack.core.model.FoodItem
import com.example.freshtrack.core.model.FoodStatus
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class FoodViewModel(private val repository: FoodRepository) : ViewModel() {
    val items: StateFlow<List<FoodItem>> = repository.items
    val history: StateFlow<List<FoodHistoryRecord>> = repository.history

    fun addFood(item: FoodItem) = viewModelScope.launch { repository.add(item) }

    /** For fixing a mistaken entry only - does not create a history record. */
    fun deleteFood(id: String) = viewModelScope.launch { repository.delete(id) }

    /** Normal removal path: records the outcome to history, then removes the item. */
    fun resolveFood(item: FoodItem, status: FoodStatus) = viewModelScope.launch {
        repository.resolve(item, status)
    }
}
