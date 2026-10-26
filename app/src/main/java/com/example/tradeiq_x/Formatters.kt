package com.example.tradeiq_x

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

object Formatters {

    private val inrFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val inrIntegerFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }

    /** Formats a numeric value as an Indian Rupee string: e.g. ₹10,00,000.00 */
    fun formatInr(amount: Double, includeDecimals: Boolean = true): String {
        val formatted = if (includeDecimals) inrFormat.format(amount) else inrIntegerFormat.format(amount)
        return "₹$formatted"
    }

    /** Formats a signed Rupee value with leading + or -: e.g. +₹1,250.00 or -₹450.50 */
    fun formatSignedAmount(amount: Double): String {
        val sign = if (amount > 0) "+" else if (amount < 0) "-" else ""
        val absVal = kotlin.math.abs(amount)
        return "$sign${formatInr(absVal)}"
    }

    /** Formats a percentage with sign and 2 decimal points: e.g. +2.34% or -0.85% */
    fun formatSignedPercent(percent: Double): String {
        val sign = if (percent > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.2f", percent)}%"
    }

    /** Formats ISO-8601 or raw date strings into readable format: 27 Oct 2026, 03:20 AM */
    fun formatTransactionDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "Recent"
        return try {
            val clean = raw.replace("Z", "").substringBefore(".")
            val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("en", "IN"))
            val date = inFormat.parse(clean)
            if (date != null) outFormat.format(date) else raw
        } catch (_: Exception) {
            raw
        }
    }
}
