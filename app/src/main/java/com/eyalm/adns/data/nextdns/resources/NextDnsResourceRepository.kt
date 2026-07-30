package com.eyalm.adns.data.nextdns.resources

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.toEmptyApiResult
import com.eyalm.adns.data.nextdns.api.toHexId
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.eyalm.adns.domain.nextdns.ApiResult

data class CustomResourceList(
    val memberships: Map<String, ResourceMembership>,
    val items: List<NextDnsResourceItem>,
)

class NextDnsResourceRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    suspend fun getMemberships(
        profileId: String,
        page: String,
        feature: String,
    ): ApiResult<Map<String, ResourceMembership>> = nextDnsApiCall {
        when (
            val result = api.getActiveListItems(profileId, page, feature)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing active resource data")
                    )
                ApiResult.Success(
                    parseResourceMemberships(data).associateBy(ResourceMembership::id),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun updateParentalMembership(
        profileId: String,
        collection: String,
        itemId: String,
        active: Boolean? = null,
        recreation: Boolean? = null,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.updateParentalResource(
            profileId = profileId,
            collection = collection,
            hexId = itemId.toHexId(),
            request = UpdateParentalResourceRequest(
                active = active,
                recreation = recreation,
            ),
        ).toEmptyApiResult()
    }

    suspend fun getServerCatalog(
        page: String,
        feature: String,
    ): ApiResult<List<NextDnsResourceItem>> = nextDnsApiCall {
        when (
            val result = api.getAvailableCatalog(page, feature)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing resource catalog data")
                    )
                ApiResult.Success(
                    mapServerResourceItems(feature, data),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getCustomList(
        profileId: String,
        page: String,
    ): ApiResult<CustomResourceList> = nextDnsApiCall {
        when (
            val result = api.getPageSettings(profileId, page)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing custom list data")
                    )
                val memberships = mutableMapOf<String, ResourceMembership>()
                val items = data.mapNotNull { element ->
                    val item = element.takeIf { it.isJsonObject }?.asJsonObject
                        ?: return@mapNotNull null
                    val id = item.get("id")
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asString
                        ?: return@mapNotNull null
                    val active = item.get("active")
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asBoolean
                        ?: true
                    memberships[id] = ResourceMembership(id = id, active = active)
                    NextDnsResourceItem(
                        id = id,
                        name = "*.$id",
                        icon = nextDnsFaviconUrl(id)
                            ?.let(ListIcon::Url)
                            ?: ListIcon.None,
                    )
                }
                ApiResult.Success(
                    CustomResourceList(memberships, items),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

}
