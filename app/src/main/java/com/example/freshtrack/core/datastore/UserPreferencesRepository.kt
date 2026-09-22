package com.example.freshtrack.core.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val daysBeforeExpirationAlert: Int = 3,
    val darkModeEnabled: Boolean = false,
)

/**
 * Domain boundary for Member 2: Notification & System Preferences storage.
 */
interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setDaysBeforeExpirationAlert(days: Int)
}

class InMemoryUserPreferencesRepository : UserPreferencesRepository {
    private val _prefs = MutableStateFlow(UserPreferences())
    override val userPreferences: Flow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(notificationsEnabled = enabled)
    }

    override suspend fun setDaysBeforeExpirationAlert(days: Int) {
        _prefs.value = _prefs.value.copy(daysBeforeExpirationAlert = days)
    }
}
