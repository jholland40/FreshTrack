package com.example.freshtrack.core.data.repository

import com.example.freshtrack.core.model.FoodItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FoodRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val _items = MutableStateFlow<List<FoodItem>>(emptyList())
    val items: StateFlow<List<FoodItem>> = _items.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.uid?.let { listenForItems(it) }
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

    suspend fun delete(id: String) {
        db.collection("items").document(id).delete().await()
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
}
