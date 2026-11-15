package com.example.freshtrack.core.model

/**
 * A record of what happened to a food item after it left the active
 * pantry. This is the "item history" the design doc's Reporting section
 * (2.7) is built from - each one is written when a pantry item is
 * resolved (marked consumed/donated/composted/discarded) instead of
 * just being deleted with no record.
 */
data class FoodHistoryRecord(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val estimatedValue: Double = 0.0,
    val status: FoodStatus = FoodStatus.DISCARDED,
    val resolvedDate: String = "",
)
