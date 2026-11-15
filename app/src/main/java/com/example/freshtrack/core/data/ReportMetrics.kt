package com.example.freshtrack.core.data

import com.example.freshtrack.core.model.FoodHistoryRecord
import com.example.freshtrack.core.model.FoodStatus
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Simple, estimate-only calculations built from item history, matching the
 * design doc's Reporting section (2.7): "These calculations will be based
 * on recorded item prices and will be presented as estimates rather than
 * exact financial records."
 *
 * Kept as plain functions over a List<FoodHistoryRecord> (rather than
 * baked into the repository or a screen) so both the home tiles and the
 * Reports screen compute the same numbers the same way, and so this is
 * easy to unit test and easy to expand later (category breakdowns, time
 * ranges, etc.) without touching the UI.
 */
object ReportMetrics {
    // Rough average price-per-pound used only to turn a dollar estimate into
    // a weight estimate for the CO2 calculation below - a loose
    // approximation, not a precise conversion, same spirit as the design
    // doc's "estimates, not exact records" framing.
    private const val AVG_PRICE_PER_LB = 3.0

    // Commonly cited average CO2e released per kg of food waste.
    private const val CO2E_KG_PER_KG_FOOD = 2.5
    private const val LB_TO_KG = 0.453592

    fun totalSaved(history: List<FoodHistoryRecord>): Double =
        history.filter { it.status == FoodStatus.CONSUMED || it.status == FoodStatus.DONATED }
            .sumOf { it.estimatedValue }

    fun totalWasted(history: List<FoodHistoryRecord>): Double =
        history.filter { it.status == FoodStatus.DISCARDED }.sumOf { it.estimatedValue }

    fun consumedCount(history: List<FoodHistoryRecord>): Int =
        history.count { it.status == FoodStatus.CONSUMED }

    fun donatedCount(history: List<FoodHistoryRecord>): Int =
        history.count { it.status == FoodStatus.DONATED }

    fun compostedCount(history: List<FoodHistoryRecord>): Int =
        history.count { it.status == FoodStatus.COMPOSTED }

    fun discardedCount(history: List<FoodHistoryRecord>): Int =
        history.count { it.status == FoodStatus.DISCARDED }

    /** Rough estimate of CO2e avoided (in kg) by not wasting the saved items. */
    fun co2AvoidedKg(history: List<FoodHistoryRecord>): Double {
        val savedValue = totalSaved(history)
        val estimatedLbs = savedValue / AVG_PRICE_PER_LB
        val estimatedKg = estimatedLbs * LB_TO_KG
        return estimatedKg * CO2E_KG_PER_KG_FOOD
    }

    /** Days since the most recent Discarded item, or null if nothing has ever been discarded. */
    fun zeroWasteStreakDays(history: List<FoodHistoryRecord>): Long? {
        val lastDiscarded = history
            .filter { it.status == FoodStatus.DISCARDED }
            .mapNotNull { record ->
                try {
                    LocalDate.parse(record.resolvedDate)
                } catch (_: DateTimeParseException) {
                    null
                }
            }
            .maxOrNull() ?: return null
        return ChronoUnit.DAYS.between(lastDiscarded, LocalDate.now())
    }
}
