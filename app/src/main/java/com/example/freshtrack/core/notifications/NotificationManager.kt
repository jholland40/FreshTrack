package com.example.freshtrack.core.notifications

import android.content.Context
import com.example.freshtrack.core.model.FoodItem

/**
 * Domain boundary for Member 2: Notification navigation and system preferences.
 * Handle channels, worker triggers, and expiration alert scheduling.
 */
interface ExpirationNotificationManager {
    fun createNotificationChannel()
    fun scheduleExpirationReminder(item: FoodItem)
    fun cancelReminder(itemId: Long)
}

class DefaultExpirationNotificationManager(
    private val context: Context,
) : ExpirationNotificationManager {

    override fun createNotificationChannel() {
        // TODO (Member 2): Set up NotificationChannel for food expiration alerts
    }

    override fun scheduleExpirationReminder(item: FoodItem) {
        // TODO (Member 2): Schedule WorkManager or AlarmManager notification
    }

    override fun cancelReminder(itemId: Long) {
        // TODO (Member 2): Cancel notification worker by ID
    }
}
