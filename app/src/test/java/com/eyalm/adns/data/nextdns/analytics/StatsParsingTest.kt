package com.eyalm.adns.data.nextdns.analytics

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
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
}
