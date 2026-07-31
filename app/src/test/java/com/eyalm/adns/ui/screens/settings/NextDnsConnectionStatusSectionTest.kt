package com.eyalm.adns.ui.screens.settings

import com.eyalm.adns.data.nextdns.connection.NextDnsConnectionProbe
import com.eyalm.adns.data.nextdns.connection.NextDnsConnectionStatus
import com.eyalm.adns.data.nextdns.connection.classifyConnectionProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextDnsConnectionStatusSectionTest {

    @Test
    fun `classifyConnectionProbe maps ok status with matching profile`() {
        val probe = NextDnsConnectionProbe(
            status = "ok",
            profile = "abc1234",
            clientName = "1.2.3.4",
            server = "server-1",
            resolver = "dns1",
            destinationIp = "10.0.0.1",
        )

        val status = classifyConnectionProbe(probe, selectedFingerprint = "abc1234")
        assertTrue(status is NextDnsConnectionStatus.UsingSelectedProfile)
        assertEquals(probe, status.probe)
    }

    @Test
    fun `classifyConnectionProbe maps status with different profile`() {
        val probe = NextDnsConnectionProbe(
            status = "ok",
            profile = "different_profile",
            clientName = "1.2.3.4",
            server = "server-1",
            resolver = "dns1",
            destinationIp = "10.0.0.1",
        )

        val status = classifyConnectionProbe(probe, selectedFingerprint = "abc1234")
        assertTrue(status is NextDnsConnectionStatus.UsingDifferentProfile)
        assertEquals("different_profile", (status as NextDnsConnectionStatus.UsingDifferentProfile).profileFingerprint)
    }

    @Test
    fun `classifyConnectionProbe maps unconfigured status`() {
        val probe = NextDnsConnectionProbe(
            status = "unconfigured",
            profile = null,
            clientName = "1.2.3.4",
            server = "server-1",
            resolver = "1.1.1.1",
            destinationIp = "10.0.0.1",
        )

        val status = classifyConnectionProbe(probe, selectedFingerprint = "abc1234")
        assertTrue(status is NextDnsConnectionStatus.NotUsingNextDns)
    }
}
