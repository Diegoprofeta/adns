package com.eyalm.adns.data.nextdns.analytics

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.NextDnsDeviceItem
import com.eyalm.adns.data.nextdns.api.NextDnsStatsGraphResponse
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.toBodyApiResult
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.JsonArray
import java.util.TimeZone

class NextDnsAnalyticsRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    suspend fun getGraph(
        profileId: String,
        scope: AnalyticsScope,
    ): ApiResult<NextDnsStatsGraphResponse> = nextDnsApiCall {
        api.getStatsGraph(
            profileId = profileId,
            period = scope.period.wireValue,
            alignment = "start",
            timezone = TimeZone.getDefault().id,
            device = scope.deviceId,
        ).toBodyApiResult()
    }

    suspend fun getCardData(
        profileId: String,
        feature: String,
        baseParams: Map<String, String>,
        scope: AnalyticsScope,
    ): ApiResult<JsonArray> = nextDnsApiCall {
        val params = buildMap {
            putAll(baseParams)
            put("from", scope.period.wireValue)
            scope.deviceId?.let { put("device", it) }
        }
        when (
            val result = api
                .getAnalyticsFeature(profileId, feature, params)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing analytics data")
                    )
                ApiResult.Success(data, result.status)
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getCardSeries(
        profileId: String,
        card: ListCard,
        scope: AnalyticsScope,
    ): ApiResult<AnalyticsSeriesResult> = nextDnsApiCall {
        if (card.feature == "domains") {
            when (val result = getGraph(profileId, scope)) {
                is ApiResult.Success -> {
                    val graph = result.value
                    val seriesPoints: List<Float>
                    val totalQueries: Int
                    val seriesList: List<NamedSeries>
                    when (card.key) {
                        "domains.blocked" -> {
                            val blocked = graph.data.firstOrNull { it.status == "blocked" }?.queries ?: emptyList()
                            seriesPoints = blocked.map { it.toFloat() }
                            totalQueries = blocked.sum()
                            seriesList = if (totalQueries > 0) listOf(NamedSeries("Blocked", seriesPoints, totalQueries)) else emptyList()
                        }
                        "domains.resolved" -> {
                            val resolved = graph.data.filter { it.status == "default" || it.status == "allowed" }
                            val maxLen = resolved.maxOfOrNull { it.queries.size } ?: 0
                            seriesPoints = (0 until maxLen).map { idx ->
                                resolved.sumOf { it.queries.getOrElse(idx) { 0 } }.toFloat()
                            }
                            totalQueries = resolved.sumOf { it.queries.sum() }
                            seriesList = if (totalQueries > 0) listOf(NamedSeries("Resolved", seriesPoints, totalQueries)) else emptyList()
                        }
                        else -> {
                            val all = graph.data
                            val maxLen = all.maxOfOrNull { it.queries.size } ?: 0
                            seriesPoints = (0 until maxLen).map { idx ->
                                all.sumOf { it.queries.getOrElse(idx) { 0 } }.toFloat()
                            }
                            totalQueries = all.sumOf { it.queries.sum() }
                            val list = mutableListOf<NamedSeries>()
                            val resolved = graph.data.filter { it.status == "default" || it.status == "allowed" }
                            val resolvedTotal = resolved.sumOf { it.queries.sum() }
                            if (resolvedTotal > 0) {
                                val rPoints = (0 until maxLen).map { idx -> resolved.sumOf { it.queries.getOrElse(idx) { 0 } }.toFloat() }
                                list.add(NamedSeries("Resolved", rPoints, resolvedTotal))
                            }
                            val blocked = graph.data.firstOrNull { it.status == "blocked" }
                            val blockedTotal = blocked?.queries?.sum() ?: 0
                            if (blockedTotal > 0) {
                                val bPoints = (0 until maxLen).map { idx -> blocked?.queries?.getOrElse(idx) { 0 }?.toFloat() ?: 0f }
                                list.add(NamedSeries("Blocked", bPoints, blockedTotal))
                            }
                            seriesList = list
                        }
                    }
                    ApiResult.Success(
                        AnalyticsSeriesResult(
                            points = seriesPoints,
                            totalQueries = totalQueries,
                            seriesList = seriesList,
                        ),
                        result.status
                    )
                }
                is ApiResult.ServerFailure -> result
                is ApiResult.NetworkFailure -> result
                is ApiResult.SerializationFailure -> result
            }
        } else {
            val params = buildMap {
                card.params["type"]?.let { put("type", it) }
                put("from", scope.period.wireValue)
                put("alignment", "start")
                put("timezone", TimeZone.getDefault().id)
                scope.deviceId?.let { put("device", it) }
            }
            when (
                val result = api
                    .getAnalyticsFeatureSeries(profileId, card.feature, params)
                    .toJsonApiResult()
            ) {
                is ApiResult.Success -> {
                    val series = parseSeries(result.value)
                    ApiResult.Success(series, result.status)
                }
                is ApiResult.ServerFailure -> result
                is ApiResult.NetworkFailure -> result
                is ApiResult.SerializationFailure -> result
            }
        }
    }

    suspend fun getExpandedCardData(
        profileId: String,
        card: ListCard,
        scope: AnalyticsScope,
        cursor: String? = null,
        limit: Int = 100,
    ): ApiResult<AnalyticsPaginatedResult> = nextDnsApiCall {
        val params = buildMap {
            putAll(card.params)
            remove("cursor")
            put("from", scope.period.wireValue)
            put("limit", limit.toString())
            cursor?.let { put("cursor", it) }
            scope.deviceId?.let { put("device", it) }
        }
        when (
            val result = api
                .getAnalyticsFeature(profileId, card.feature, params)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing analytics data")
                    )
                val rows = parseList(card.copy(limit = null), data)
                val nextCursor = result.value
                    .getAsJsonObject("meta")
                    ?.getAsJsonObject("pagination")
                    ?.get("cursor")
                    ?.takeIf { !it.isJsonNull }
                    ?.asString
                ApiResult.Success(
                    AnalyticsPaginatedResult(
                        rows = rows,
                        cursor = nextCursor,
                    ),
                    result.status
                )
            }
            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getDevices(profileId: String): ApiResult<List<NextDnsDeviceItem>> = nextDnsApiCall {
        when (
            val result = api
                .getDevices(profileId)
                .toBodyApiResult()
        ) {
            is ApiResult.Success -> ApiResult.Success(result.value.data, result.status)
            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

}

