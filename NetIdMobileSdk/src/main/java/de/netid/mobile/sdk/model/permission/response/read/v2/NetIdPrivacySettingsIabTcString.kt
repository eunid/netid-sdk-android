package de.netid.mobile.sdk.model.permission.response.read.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetIdPrivacySettingsIabTcString(
    val value: String,
    @SerialName("changed_at")
    val changedAt: String
)
