package com.example.freshtrack.ui

import java.util.UUID

data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val expiryDate: String,
    val quantity: Int = 1,
    val isExpiringSoon: Boolean = false
)
