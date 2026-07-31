package com.eyalm.adns.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountSettingsRefreshTest {

    @Test
    fun `pull refresh triggers profile and connection refresh independently`() {
        val refreshes = mutableListOf<String>()

        refreshAccountSettings(
            refreshProfiles = { refreshes += "profiles" },
            refreshConnection = { refreshes += "connection" },
        )

        assertEquals(listOf("profiles", "connection"), refreshes)
    }
}
