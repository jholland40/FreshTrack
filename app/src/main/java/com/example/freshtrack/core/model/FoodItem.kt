package com.example.freshtrack.core.model

data class FoodItem(
    val id: String = "",
    val name: String,
    val category: String,
    val quantity: Int,
    val expirationDate: String,
    val location: String,
    val barcode: String = "",
    val estimatedValue: Double = 0.0,
) {
    companion object {
        fun fromProductInfo(
            info: ProductInfo,
            quantity: Int,
            expirationDate: String,
            location: String,
        ): FoodItem = FoodItem(
            id = "",
            name = info.name,
            category = info.category ?: "Other",
            quantity = quantity,
            expirationDate = expirationDate,
            location = location,
            barcode = info.barcode,
            estimatedValue = 0.0,
        )
    }
}
