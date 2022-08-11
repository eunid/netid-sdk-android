package de.netid.mobile.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Permissions(
    @SerialName("status_code")
    val statusCode: PermissionStatusCode,
    @SerialName("subject_identifiers")
    val subjectIdentifiers: SubjectIdentifiers,
    @SerialName("netid_privacy_settings")
    val netIdPrivacySettings: List<NetIdPrivacySettings>
) {
    override fun toString(): String {
        return "Permissions(statusCode=$statusCode, subjectIdentifiers=$subjectIdentifiers, netIdPrivacySettings=$netIdPrivacySettings)"
    }
}
