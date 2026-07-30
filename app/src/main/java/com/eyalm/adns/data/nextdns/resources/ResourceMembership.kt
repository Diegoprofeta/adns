package com.eyalm.adns.data.nextdns.resources

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName

data class ResourceMembership(
    val id: String,
    val active: Boolean = true,
    val recreation: Boolean? = null,
)

data class UpdateParentalResourceRequest(
    @SerializedName("active") val active: Boolean? = null,
    @SerializedName("recreation") val recreation: Boolean? = null,
)

fun parseResourceMemberships(data: JsonArray): List<ResourceMembership> =
    data.mapNotNull { element ->
        val item = element.takeIf { it.isJsonObject }?.asJsonObject
            ?: return@mapNotNull null
        val id = item.get("id")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?: return@mapNotNull null
        ResourceMembership(
            id = id,
            active = item.get("active")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
                ?.asBoolean
                ?: true,
            recreation = item.get("recreation")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
                ?.asBoolean,
        )
    }

fun Map<String, ResourceMembership>.withMembership(
    membership: ResourceMembership,
): Map<String, ResourceMembership> = this + (membership.id to membership)

fun Map<String, ResourceMembership>.withActive(
    itemId: String,
    active: Boolean,
): Map<String, ResourceMembership> {
    val current = get(itemId) ?: return this
    return withMembership(current.copy(active = active))
}

fun Map<String, ResourceMembership>.withRecreation(
    itemId: String,
    recreation: Boolean,
): Map<String, ResourceMembership> {
    val current = get(itemId) ?: return this
    return withMembership(current.copy(recreation = recreation))
}
