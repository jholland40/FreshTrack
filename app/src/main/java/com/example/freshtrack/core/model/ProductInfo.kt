package com.example.freshtrack.core.model

/**
 * What a barcode/product lookup (Open Food Facts or PLU Pro) hands back.
 * Owned by Member 3's scanner feature - this is just the shared shape
 * so their lookup code and our Firestore layer agree on what a "product" is.
 */
data class ProductInfo(
    val barcode: String,
    val name: String,
    val category: String? = null,
    val brand: String? = null,
    val imageUrl: String? = null,
    val source: String, // "OpenFoodFacts" or "PLUPro"
)