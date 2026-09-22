package com.example.freshtrack.core.model

data class FoodItem(
    val id: Long = 0,
    val name: String,
    val category: String,
    val quantity: Int,
    val expirationDate: String,
    val location: String,
    val barcode: String = "",
    val estimatedValue: Double = 0.0,
)
