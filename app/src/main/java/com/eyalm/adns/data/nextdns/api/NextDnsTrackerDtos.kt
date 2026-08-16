package com.eyalm.adns.data.nextdns.api

import com.google.gson.annotations.SerializedName

data class TrackerInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String? = null,
    @SerializedName("website") val website: String? = null,
    @SerializedName("company") val company: TrackerCompany? = null,
    @SerializedName("prevalence") val prevalence: Float? = null,
)

data class TrackerCompany(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("privacyUrl") val privacyUrl: String? = null,
)
