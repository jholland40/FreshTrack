package com.example.freshtrack.core.data.repository

import com.example.freshtrack.core.model.FoodHistoryRecord
import com.example.freshtrack.core.model.FoodItem
import com.example.freshtrack.core.model.FoodStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    // Surfaces write failures to the UI instead of failing silently. Before
    // this, if a device wasn't signed in yet (e.g. anonymous auth hadn't
    // completed), add/resolve/delete just did nothing with no error anywhere
    // - which looked exactly like "my item didn't save" with zero clue why.
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    // Tracks the currently-attached items/history listeners so we can detach
    // them before attaching new ones, and so we never attach a second,
    // redundant pair for the SAME uid. Without this guard, every time
    // Firebase's auth-state listener fired with a uid - which can legitimately
    // happen more than once per session, and definitely happens if sign-in
    // is ever (incorrectly) retried mid-session - this code attached a brand
    // new pair of real-time listeners on top of whichever ones were already
    // running, with no cleanup. Two things followed from that: the growing
    // pile of never-removed listeners is a real memory leak (worse the longer
    // a session runs and the more it's already competing for memory, e.g.
    // while screen recording is also active), and if a new listener pair was
    // ever attached for a DIFFERENT uid, the old and new listeners raced to
    // overwrite _items/_history - which, if the new uid had no data yet,
    // could make a user's whole pantry flash empty mid-session: it looks
    // exactly like being logged out.
    private var itemsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var listenersForUid: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                detachListeners()
                return@addAuthStateListener
            }
            if (uid == listenersForUid) {
                // Already listening for exactly this uid - do nothing.
                return@addAuthStateListener
            }
            detachListeners()
            listenersForUid = uid
            listenForItems(uid)
            listenForHistory(uid)
        }
    }

    private fun detachListeners() {
        itemsListener?.remove()
        historyListener?.remove()
        itemsListener = null
        historyListener = null
        listenersForUid = null
    }

    private fun listenForItems(uid: String) {
        itemsListener = db.collection("items")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _items.value = snapshot.documents.mapNotNull { it.toFoodItem() }
                        .sortedBy { it.expirationDate }
                }
            }
    }

    private fun listenForHistory(uid: String) {
        historyListener = db.collection("history")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _history.value = snapshot.documents.mapNotNull { it.toHistoryRecord() }
                        .sortedByDescending { it.resolvedDate }
                }
            }
    }

    suspend fun add(item: FoodItem) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _errorMessage.value = "Not signed in yet - check your internet connection, then reopen the app."
            return
        }
        try {
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
        } catch (e: Exception) {
            _errorMessage.value = "Couldn't save item: ${e.message ?: "unknown error"}"
        }
    }

    /**
     * Hard delete with no history record - only for correcting a mistaken
     * entry (e.g. a duplicate or typo). Normal removal from the pantry
     * should go through resolve() instead, so Reports stays accurate.
     */
    suspend fun delete(id: String) {
        try {
            db.collection("items").document(id).delete().await()
        } catch (e: Exception) {
            _errorMessage.value = "Couldn't remove item: ${e.message ?: "unknown error"}"
        }
    }

    /**
     * Removes an item from the active pantry and records what actually
     * happened to it, so the Reports screen has real history to work from.
     * This is what the pantry UI's "remove" action calls in normal use.
     */
    suspend fun resolve(item: FoodItem, status: FoodStatus) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _errorMessage.value = "Not signed in yet - check your internet connection, then reopen the app."
            return
        }
        try {
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
        } catch (e: Exception) {
            _errorMessage.value = "Couldn't update item: ${e.message ?: "unknown error"}"
        }
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
