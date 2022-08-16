package de.netid.mobile.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PermissionUpdateResponse(
    @SerialName("subject_identifiers")
    val subjectIdentifiers: SubjectIdentifiers
) {
    override fun toString(): String {
        return "PermissionUpdateResponse(subjectIdentifiers=$subjectIdentifiers)"
    }
}
