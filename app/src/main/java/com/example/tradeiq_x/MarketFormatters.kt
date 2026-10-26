package com.example.tradeiq_x

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Shared formatting utilities for market data screens and components.
 */
object MarketFormatters {

    /**
     * Formats an ISO-8601 UTC timestamp string into a human-readable relative string
     * such as "Just now", "2m ago", "1h ago", or "3d ago".
     *
     * @param isoTimestamp ISO-8601 timestamp string (e.g. "2026-10-26T10:15:30.123456+00:00").
     * @param currentMillis Current time in milliseconds for relative difference calculation.
     * @return Formatted relative time string.
     */
    fun formatRelativeTime(
        isoTimestamp: String?,
        currentMillis: Long = System.currentTimeMillis()
    ): String {
        if (isoTimestamp.isNullOrBlank()) {
            return "Updated recently"
        }

        val timestampMs = try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleanIso = isoTimestamp.substringBefore('.').substringBefore('Z').substringBefore('+')
            format.parse(cleanIso)?.time ?: return "Updated recently"
        } catch (_: Exception) {
            return "Updated recently"
        }

        val diffMs = currentMillis - timestampMs
        if (diffMs < 0L) return "Just now"

        val seconds = diffMs / 1000L
        val minutes = seconds / 60L
        val hours = minutes / 60L
        val days = hours / 24L

        return when {
            seconds < 45 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }
}
