package com.eyalm.adns.data.nextdns.analytics

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsParsingTest {

    @Test
    fun `parseList parses protocols correctly`() {
        val json = """
            [
              {"protocol": "DNS-over-HTTPS", "queries": 958757},
              {"protocol": "DNS-over-TLS", "queries": 39582},
              {"protocol": "UDP", "queries": 2334}
            ]
        """.trimIndent()
        val data = JsonParser.parseString(json).asJsonArray
        val card = StatsRegistry.cards.first { it.key == "protocols" } as ListCard

        val rows = parseList(card, data)

        assertEquals(3, rows.size)
        assertEquals("DNS-over-HTTPS", rows[0].id)
        assertEquals("DNS-over-HTTPS", rows[0].title)
        assertEquals("958,757", rows[0].value)

        assertEquals("DNS-over-TLS", rows[1].id)
        assertEquals("DNS-over-TLS", rows[1].title)
        assertEquals("39,582", rows[1].value)

        assertEquals("UDP", rows[2].id)
        assertEquals("UDP", rows[2].title)
        assertEquals("2,334", rows[2].value)
    }

    @Test
    fun `parseList parses queryTypes correctly`() {
        val json = """
            [
              {"type": 28, "name": "AAAA", "queries": 356230},
              {"type": 1, "name": "A", "queries": 341812},
              {"type": 65, "name": "HTTPS", "queries": 260478}
            ]
        """.trimIndent()
        val data = JsonParser.parseString(json).asJsonArray
        val card = StatsRegistry.cards.first { it.key == "queryTypes" } as ListCard

        val rows = parseList(card, data)

        assertEquals(3, rows.size)
        assertEquals("AAAA", rows[0].id)
        assertEquals("AAAA", rows[0].title)
        assertEquals("356,230", rows[0].value)

        assertEquals("A", rows[1].id)
        assertEquals("A", rows[1].title)
        assertEquals("341,812", rows[1].value)

        assertEquals("HTTPS", rows[2].id)
        assertEquals("HTTPS", rows[2].title)
        assertEquals("260,478", rows[2].value)
    }

    @Test
    fun `parseList parses ipVersions correctly`() {
        val json = """
            [
              {"version": 6, "queries": 784154},
              {"version": 4, "queries": 174308}
            ]
        """.trimIndent()
        val data = JsonParser.parseString(json).asJsonArray
        val card = StatsRegistry.cards.first { it.key == "ipVersions" } as ListCard

        val rows = parseList(card, data)

        assertEquals(2, rows.size)
        assertEquals("IPv6", rows[0].id)
        assertEquals("IPv6", rows[0].title)
        assertEquals("784,154", rows[0].value)

        assertEquals("IPv4", rows[1].id)
        assertEquals("IPv4", rows[1].title)
        assertEquals("174,308", rows[1].value)
    }

    @Test
    fun `parseSeries aggregates multiple items across tumbling windows`() {
        val json = """
            {
              "data": [
                { "domain": "a.com", "queries": [10, 20, 30, 40] },
                { "domain": "b.com", "queries": [5, 15, 25, 35] },
                { "domain": "c.com", "queries": [1, 2, 3, 4] }
              ],
              "meta": {
                "series": {
                  "times": ["2026-08-01", "2026-08-02", "2026-08-03", "2026-08-04"],
                  "interval": 86400
                }
              }
            }
        """.trimIndent()
        val jsonObject = JsonParser.parseString(json).asJsonObject
        val result = parseSeries(jsonObject)

        assertEquals(listOf(16f, 37f, 58f, 79f), result.points)
        assertEquals(190, result.totalQueries)
        assertEquals(3, result.seriesList.size)
        assertEquals("a.com", result.seriesList[0].name)
        assertEquals(100, result.seriesList[0].totalQueries)
        assertEquals(listOf(10f, 20f, 30f, 40f), result.seriesList[0].points)
        assertEquals("b.com", result.seriesList[1].name)
        assertEquals(80, result.seriesList[1].totalQueries)
        assertEquals("c.com", result.seriesList[2].name)
        assertEquals(10, result.seriesList[2].totalQueries)
    }

    @Test
    fun `parseSeries caps at 6 lines and groups extra into Other`() {
        val json = """
            {
              "data": [
                { "name": "Item1", "queries": [100] },
                { "name": "Item2", "queries": [90] },
                { "name": "Item3", "queries": [80] },
                { "name": "Item4", "queries": [70] },
                { "name": "Item5", "queries": [60] },
                { "name": "Item6", "queries": [50] },
                { "name": "Item7", "queries": [40] },
                { "name": "Item8", "queries": [30] }
              ]
            }
        """.trimIndent()
        val jsonObject = JsonParser.parseString(json).asJsonObject
        val result = parseSeries(jsonObject)

        assertEquals(6, result.seriesList.size)
        assertEquals("Item1", result.seriesList[0].name)
        assertEquals(100, result.seriesList[0].totalQueries)
        assertEquals("Item5", result.seriesList[4].name)
        assertEquals(60, result.seriesList[4].totalQueries)
        assertEquals("Other", result.seriesList[5].name)
        assertEquals(120, result.seriesList[5].totalQueries) // 50 + 40 + 30
        assertEquals(listOf(120f), result.seriesList[5].points)
    }

    @Test
    fun `parseSeries handles empty or missing data gracefully`() {
        val emptyJson = JsonParser.parseString("""{"data": []}""").asJsonObject
        val result = parseSeries(emptyJson)

        assertEquals(emptyList<Float>(), result.points)
        assertEquals(0, result.totalQueries)
        assertTrue(result.seriesList.isEmpty())
    }

    @Test
    fun `parseList parses domains with tracker and rawQueries`() {
        val json = """
            [
              {"domain": "clients2.google.com", "tracker": "google", "queries": 1162},
              {"domain": "example.com", "queries": 450}
            ]
        """.trimIndent()
        val data = JsonParser.parseString(json).asJsonArray
        val card = StatsRegistry.cards.first { it.key == "domains.resolved" } as ListCard

        val rows = parseList(card, data)

        assertEquals(2, rows.size)
        assertEquals("clients2.google.com", rows[0].id)
        assertEquals("clients2.google.com", rows[0].title)
        assertEquals("1,162", rows[0].value)
        assertEquals(1162, rows[0].rawQueries)
        assertEquals("google", rows[0].tracker)

        assertEquals("example.com", rows[1].id)
        assertEquals("450", rows[1].value)
        assertEquals(450, rows[1].rawQueries)
        assertEquals(null, rows[1].tracker)
    }

    @Test
    fun `parseList parses GAFAM with percentage and query subtitle`() {
        val json = """
            [
              {"company": "google", "queries": 300},
              {"company": "apple", "queries": 100}
            ]
        """.trimIndent()
        val data = JsonParser.parseString(json).asJsonArray
        val card = StatsRegistry.cards.first { it.key == "destinations.gafam" } as ListCard

        val rows = parseList(card, data)

        assertEquals(2, rows.size)
        assertEquals("google", rows[0].id)
        assertEquals(75f, rows[0].percentage ?: 0f, 0.01f)
        assertEquals("75%", rows[0].value)
        assertEquals(300, rows[0].rawQueries)

        assertEquals("apple", rows[1].id)
        assertEquals(25f, rows[1].percentage ?: 0f, 0.01f)
        assertEquals("25%", rows[1].value)
        assertEquals(100, rows[1].rawQueries)
    }
}
