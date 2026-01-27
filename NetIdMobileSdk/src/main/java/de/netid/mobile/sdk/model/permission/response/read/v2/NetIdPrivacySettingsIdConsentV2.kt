package de.netid.mobile.sdk.model.permission.response.read.v2

import de.netid.mobile.sdk.model.NetIdPermissionStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetIdPrivacySettingsIdConsentV2(
    val status: NetIdPermissionStatus,
    @SerialName("changed_at")
    val changedAt: String
)
