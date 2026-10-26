package com.example.tradeiq_x

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Shared formatting utilities for market data screens and components.
 */
object MarketFormatters {

    /**
     * Formats an INR price value with 2 decimals and Indian grouping (e.g. "₹1,450.25").
     */
    fun formatInrPrice(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "₹" + formatter.format(amount)
    }

    /**
     * Formats an absolute price change with sign prefix and 2 decimals (e.g. "+₹12.50", "-₹5.20").
     */
    fun formatPriceChange(change: Double): String {
        val prefix = if (change > 0.0) "+" else if (change < 0.0) "-" else ""
        val absVal = abs(change)
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return prefix + "₹" + formatter.format(absVal)
    }

    /**
     * Formats a percentage change with sign prefix and 2 decimals (e.g. "+1.32%", "-0.42%", "0.00%").
     */
    fun formatPercentChange(percent: Double): String {
        val prefix = if (percent > 0.0) "+" else if (percent < 0.0) "-" else ""
        val absVal = abs(percent)
        return prefix + String.format(Locale.US, "%.2f%%", absVal)
    }

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
