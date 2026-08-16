package com.eyalm.adns.viewmodel.nextdns

import com.eyalm.adns.data.nextdns.analytics.AnalyticsPeriod
import com.eyalm.adns.data.nextdns.analytics.AnalyticsScope
import com.eyalm.adns.data.nextdns.analytics.AnalyticsSeriesResult
import com.eyalm.adns.data.nextdns.analytics.ListCard
import com.eyalm.adns.data.nextdns.analytics.ListKind
import com.eyalm.adns.data.nextdns.analytics.StatRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCardDetailUiStateTest {

    @Test
    fun `default state has expected initial values`() {
        val state = StatsCardDetailUiState()

        assertEquals(null, state.profileId)
        assertEquals(null, state.card)
        assertEquals(AnalyticsScope(), state.scope)
        assertTrue(state.items.isEmpty())
        assertTrue(state.series.points.isEmpty())
        assertEquals(0, state.series.totalQueries)
        assertFalse(state.initialLoading)
        assertFalse(state.seriesLoading)
        assertFalse(state.loadingNextPage)
        assertFalse(state.refreshing)
        assertTrue(state.hasMorePages)
        assertEquals(null, state.error)
    }

    @Test
    fun `state update maintains integrity across items and series`() {
        val card = ListCard(
            key = "domains.blocked",
            feature = "domains",
            params = mapOf("status" to "blocked"),
            localePath = emptyList(),
            emptyPath = emptyList(),
            kind = ListKind.DOMAINS,
        )
        val scope = AnalyticsScope(period = AnalyticsPeriod.Days7, deviceId = "dev-1")
        val rows = listOf(
            StatRow(id = "ads.com", title = "ads.com", value = "1,500"),
            StatRow(id = "track.com", title = "track.com", value = "900")
        )
        val series = AnalyticsSeriesResult(
            points = listOf(100f, 250f, 400f),
            totalQueries = 2400
        )

        val state = StatsCardDetailUiState(
            profileId = "prof-123",
            card = card,
            scope = scope,
            items = rows,
            series = series,
            initialLoading = false,
            seriesLoading = false,
            loadingNextPage = false,
            hasMorePages = true
        )

        assertEquals("prof-123", state.profileId)
        assertEquals("domains.blocked", state.card?.key)
        assertEquals(AnalyticsPeriod.Days7, state.scope.period)
        assertEquals("dev-1", state.scope.deviceId)
        assertEquals(2, state.items.size)
        assertEquals(listOf(100f, 250f, 400f), state.series.points)
        assertEquals(2400, state.series.totalQueries)
        assertTrue(state.hasMorePages)
    }

    @Test
    fun `enabledSeriesNames null by default and can be empty set`() {
        val state = StatsCardDetailUiState()
        assertEquals(null, state.enabledSeriesNames)

        val disabledAllState = state.copy(enabledSeriesNames = emptySet())
        assertEquals(emptySet<String>(), disabledAllState.enabledSeriesNames)
    }

    @Test
    fun `isGraphSupported correctly identifies supported graph cards`() {
        val gafamCard = ListCard(
            key = "destinations.gafam",
            feature = "destinations",
            params = mapOf("type" to "gafam"),
            localePath = emptyList(),
            emptyPath = emptyList(),
            kind = ListKind.GAFAM
        )
        assertTrue(isGraphSupported(gafamCard))

        val devicesCard = ListCard(
            key = "devices",
            feature = "devices",
            params = emptyMap(),
            localePath = emptyList(),
            emptyPath = emptyList(),
            kind = ListKind.DEVICES
        )
        assertTrue(isGraphSupported(devicesCard))

        val domainsCard = ListCard(
            key = "domains.resolved",
            feature = "domains",
            params = emptyMap(),
            localePath = emptyList(),
            emptyPath = emptyList(),
            kind = ListKind.DOMAINS
        )
        assertFalse(isGraphSupported(domainsCard))
    }
}
