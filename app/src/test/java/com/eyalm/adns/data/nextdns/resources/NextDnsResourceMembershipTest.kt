package com.eyalm.adns.data.nextdns.resources

import com.eyalm.adns.data.nextdns.nextDnsFixture
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class NextDnsResourceMembershipTest {
    @Test
    fun `membership parser preserves inactive and recreation state`() {
        val data = JsonParser.parseString(
            nextDnsFixture("parental-control/memberships.json")
        ).asJsonObject.getAsJsonArray("data")

        assertEquals(
            listOf(
                ResourceMembership("roblox", active = false, recreation = true),
                ResourceMembership("social-networks", active = true, recreation = false),
            ),
            parseResourceMemberships(data),
        )
    }

    @Test
    fun `missing active defaults true for id-only lists`() {
        val data = JsonParser.parseString("""[{"id":"work"}]""").asJsonArray

        assertEquals(
            ResourceMembership("work", active = true),
            parseResourceMemberships(data).single(),
        )
    }
}
