package com.mahdiMb55.prices.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DesignSystemTest {
    @Test
    fun lightSchemeUsesAccessiblePricesBrandRoles() {
        assertEquals(Color(0xFF006B60), PricesLightColorScheme.primary)
        assertEquals(Color.White, PricesLightColorScheme.onPrimary)
        assertNotEquals(PricesLightColorScheme.primary, PricesLightColorScheme.primaryContainer)
        assertEquals(Color(0xFF9CF2E2), PricesLightColorScheme.primaryContainer)
    }

    @Test
    fun darkSchemeUsesDistinctBrandAndSurfaceRoles() {
        assertEquals(Color(0xFF5DDCC7), PricesDarkColorScheme.primary)
        assertEquals(Color(0xFF003731), PricesDarkColorScheme.onPrimary)
        assertNotEquals(PricesDarkColorScheme.background, PricesDarkColorScheme.surface)
        assertNotEquals(PricesLightColorScheme.primary, PricesDarkColorScheme.primary)
    }

    @Test
    fun semanticStatusColorsAreAvailable() {
        assertEquals(Color(0xFF2E7D32), PricesColors.success)
        assertEquals(Color(0xFFBA1A1A), PricesColors.error)
        assertEquals(Color(0xFF8A5100), PricesColors.warning)
        assertEquals(Color(0xFF00658C), PricesColors.information)
    }
}
