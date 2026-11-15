package com.example.freshtrack.core.model

/**
 * The five item outcomes from the design doc's "Item Status and History"
 * section (2.6). AVAILABLE is the state an item is in while it's still
 * sitting in the active pantry; the other four are recorded to history
 * once an item is resolved (removed) and no longer appears in the live
 * inventory list.
 */
enum class FoodStatus {
    AVAILABLE,
    CONSUMED,
    DONATED,
    COMPOSTED,
    DISCARDED,
}
