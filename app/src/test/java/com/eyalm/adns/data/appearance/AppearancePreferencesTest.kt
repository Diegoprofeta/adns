package com.eyalm.adns.data.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun `system dark mode follows the system`() {
        assertTrue(resolveDarkTheme(DarkModePreference.System, systemDark = true))
        assertFalse(resolveDarkTheme(DarkModePreference.System, systemDark = false))
    }

    @Test
    fun `explicit dark light and oled modes ignore the system`() {
        assertTrue(resolveDarkTheme(DarkModePreference.Dark, systemDark = false))
        assertTrue(resolveDarkTheme(DarkModePreference.Oled, systemDark = false))
        assertFalse(resolveDarkTheme(DarkModePreference.Light, systemDark = true))
    }

    @Test
    fun `invalid stored values return stable defaults and oled is parsed`() {
        assertEquals(DarkModePreference.System, DarkModePreference.fromStored("invalid"))
        assertEquals(DarkModePreference.Oled, DarkModePreference.fromStored("oled"))
        assertEquals(ColorSchemePreference.Adns, ColorSchemePreference.fromStored("invalid"))
    }

    @Test
    fun `dynamic colors require android twelve`() {
        assertFalse(supportsDynamicColor(sdkInt = 30))
        assertTrue(supportsDynamicColor(sdkInt = 31))
    }
}
