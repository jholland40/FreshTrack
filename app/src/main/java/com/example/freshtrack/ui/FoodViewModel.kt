package com.example.freshtrack.ui

import androidx.lifecycle.ViewModel
import com.example.freshtrack.core.data.repository.FoodRepository
import com.example.freshtrack.core.model.FoodItem
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class FoodViewModel(private val repository: FoodRepository) : ViewModel() {
    val items: StateFlow<List<FoodItem>> = repository.items
    fun addFood(item: FoodItem) = viewModelScope.launch { repository.add(item) }
    fun deleteFood(id: String) = viewModelScope.launch { repository.delete(id) }
}
