package de.netid.mobile.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetIdPrivacySettings(
    val type: String,
    val status: String = "",
    val value: String = "",
    @SerialName("changed_at")
    val changedAt: String
) {
    override fun toString(): String {
        return "NetIdPrivacySettings(type='$type', status='$status', value='$value', changedAt='$changedAt')"
    }
}