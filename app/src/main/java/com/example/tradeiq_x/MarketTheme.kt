package com.example.tradeiq_x

import androidx.compose.ui.graphics.Color

/**
 * Consolidated market change colors and background tints for consistent UI indication
 * across light and dark market data screens.
 */
object MarketColors {
    // Primary green gain and red loss palette
    val GainGreen = Color(0xFF16A34A)
    val GainGreenLight = Color(0xFFDCFCE7)
    val LossRed = Color(0xFFDC2626)
    val LossRedLight = Color(0xFFFEE2E2)
    val NeutralGrey = Color(0xFF64748B)

    // Dark theme variant accents
    val DarkGainGreen = Color(0xFF32D49A)
    val DarkLossRed = Color(0xFFFF5364)

    /**
     * Resolves the primary foreground text/icon color for a change indicator.
     */
    fun changeColor(isPositive: Boolean): Color =
        if (isPositive) GainGreen else LossRed

    /**
     * Resolves the pill/badge container background color for a change indicator.
     */
    fun changeBackground(isPositive: Boolean): Color =
        if (isPositive) GainGreenLight else LossRedLight

    /**
     * Resolves the dark-theme accent color for a change indicator or chart bar.
     */
    fun darkChangeColor(isPositive: Boolean): Color =
        if (isPositive) DarkGainGreen else DarkLossRed
}
