package com.eyalm.adns.data.nextdns.connection

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface NextDnsConnectionApi {
    @GET
    suspend fun probe(@Url url: String): Response<NextDnsConnectionProbe>

    @GET("resolver/{resolver}")
    suspend fun resolver(@Path("resolver") resolver: String): Response<NextDnsResolver>
}

data class NextDnsConnectionProbe(
    @SerializedName("status") val status: String,
    @SerializedName("protocol") val protocol: String? = null,
    @SerializedName("profile") val profile: String? = null,
    @SerializedName("resolver") val resolver: String? = null,
    @SerializedName("srcIP") val sourceIp: String? = null,
    @SerializedName("destIP") val destinationIp: String? = null,
    @SerializedName("anycast") val anycast: Boolean? = null,
    @SerializedName("server") val server: String? = null,
    @SerializedName("clientName") val clientName: String? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
    @SerializedName("deviceID") val deviceId: String? = null,
)

data class NextDnsResolver(
    @SerializedName("name") val name: String? = null,
)
