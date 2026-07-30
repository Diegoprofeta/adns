package com.eyalm.adns.data.nextdns.resources

import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceMembershipMutationTest {
    @Test
    fun `active mutation preserves recreation and can restore prior membership`() {
        val before = ResourceMembership("roblox", active = true, recreation = true)
        val memberships = mapOf(before.id to before)

        val optimistic = memberships.withActive(before.id, active = false)

        assertEquals(
            ResourceMembership("roblox", active = false, recreation = true),
            optimistic[before.id],
        )
        assertEquals(memberships, optimistic.withMembership(before))
    }

    @Test
    fun `recreation mutation preserves active state`() {
        val memberships = mapOf(
            "roblox" to ResourceMembership("roblox", active = false, recreation = false)
        )

        assertEquals(
            ResourceMembership("roblox", active = false, recreation = true),
            memberships.withRecreation("roblox", recreation = true)["roblox"],
        )
    }

    @Test
    fun `mutation of missing membership is ignored`() {
        val memberships = emptyMap<String, ResourceMembership>()

        assertEquals(memberships, memberships.withActive("missing", active = false))
        assertEquals(memberships, memberships.withRecreation("missing", recreation = true))
    }
}
