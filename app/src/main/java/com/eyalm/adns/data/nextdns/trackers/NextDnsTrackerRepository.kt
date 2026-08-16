package com.eyalm.adns.data.nextdns.trackers

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.TrackerInfo
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.toBodyApiResult
import com.eyalm.adns.domain.nextdns.ApiResult
import java.util.concurrent.ConcurrentHashMap

class NextDnsTrackerRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    private val cache = ConcurrentHashMap<String, TrackerInfo>()

    suspend fun getTrackerInfo(trackerId: String): ApiResult<TrackerInfo> {
        val trimmed = trackerId.trim().lowercase()
        if (trimmed.isEmpty()) {
            return ApiResult.SerializationFailure(IllegalArgumentException("Empty tracker ID"))
        }
        cache[trimmed]?.let {
            return ApiResult.Success(it, 200)
        }

        return nextDnsApiCall {
            val response = api.getTrackerInfo(trimmed).toBodyApiResult()
            if (response is ApiResult.Success) {
                cache[trimmed] = response.value
            }
            response
        }
    }
}
