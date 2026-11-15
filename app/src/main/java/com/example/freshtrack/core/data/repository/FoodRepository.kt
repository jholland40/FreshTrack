package com.example.freshtrack.core.data.repository

import com.example.freshtrack.core.model.FoodHistoryRecord
import com.example.freshtrack.core.model.FoodItem
import com.example.freshtrack.core.model.FoodStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class FoodRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val _items = MutableStateFlow<List<FoodItem>>(emptyList())
    val items: StateFlow<List<FoodItem>> = _items.asStateFlow()

    private val _history = MutableStateFlow<List<FoodHistoryRecord>>(emptyList())
    val history: StateFlow<List<FoodHistoryRecord>> = _history.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.uid?.let { uid ->
                listenForItems(uid)
                listenForHistory(uid)
            }
        }
    }

    private fun listenForItems(uid: String) {
        db.collection("items")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _items.value = snapshot.documents.mapNotNull { it.toFoodItem() }
                        .sortedBy { it.expirationDate }
                }
            }
    }

    private fun listenForHistory(uid: String) {
        db.collection("history")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _history.value = snapshot.documents.mapNotNull { it.toHistoryRecord() }
                        .sortedByDescending { it.resolvedDate }
                }
            }
    }

    suspend fun add(item: FoodItem) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "name" to item.name,
            "category" to item.category,
            "quantity" to item.quantity,
            "expirationDate" to item.expirationDate,
            "location" to item.location,
            "barcode" to item.barcode,
            "estimatedValue" to item.estimatedValue,
            "userId" to uid,
        )
        db.collection("items").add(data).await()
    }

    /**
     * Hard delete with no history record - only for correcting a mistaken
     * entry (e.g. a duplicate or typo). Normal removal from the pantry
     * should go through resolve() instead, so Reports stays accurate.
     */
    suspend fun delete(id: String) {
        db.collection("items").document(id).delete().await()
    }

    /**
     * Removes an item from the active pantry and records what actually
     * happened to it, so the Reports screen has real history to work from.
     * This is what the pantry UI's "remove" action calls in normal use.
     */
    suspend fun resolve(item: FoodItem, status: FoodStatus) {
        val uid = auth.currentUser?.uid ?: return
        val historyData = hashMapOf(
            "name" to item.name,
            "category" to item.category,
            "estimatedValue" to item.estimatedValue,
            "status" to status.name,
            "resolvedDate" to LocalDate.now().toString(),
            "userId" to uid,
        )
        db.collection("history").add(historyData).await()
        db.collection("items").document(item.id).delete().await()
    }

    private fun DocumentSnapshot.toFoodItem(): FoodItem? {
        return try {
            FoodItem(
                id = id,
                name = getString("name") ?: return null,
                category = getString("category") ?: "Other",
                quantity = (getLong("quantity") ?: 1L).toInt(),
                expirationDate = getString("expirationDate") ?: "",
                location = getString("location") ?: "",
                barcode = getString("barcode") ?: "",
                estimatedValue = getDouble("estimatedValue") ?: 0.0,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toHistoryRecord(): FoodHistoryRecord? {
        return try {
            FoodHistoryRecord(
                id = id,
                name = getString("name") ?: return null,
                category = getString("category") ?: "Other",
                estimatedValue = getDouble("estimatedValue") ?: 0.0,
                status = try {
                    FoodStatus.valueOf(getString("status") ?: "DISCARDED")
                } catch (e: IllegalArgumentException) {
                    FoodStatus.DISCARDED
                },
                resolvedDate = getString("resolvedDate") ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }
}
