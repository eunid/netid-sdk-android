package de.netid.mobile.sdk.model

import kotlinx.serialization.Serializable

@Serializable
enum class NetIdPrivacySettingsStatus(val status: String) {
    Valid("VALID"),
    Invalid("INVALID")
}
