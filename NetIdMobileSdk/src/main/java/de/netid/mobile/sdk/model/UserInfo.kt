package de.netid.mobile.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val sub: String,
    val givenName: String,
    val familyName: String,
    val birthDate: String
) {
    override fun toString(): String {
        return "UserInfo(sub='$sub', givenName='$givenName', familyName='$familyName', birthDate='$birthDate')"
    }
}