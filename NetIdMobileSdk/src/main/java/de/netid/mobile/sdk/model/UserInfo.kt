package de.netid.mobile.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val sub: String,
    @SerialName("given_name")
    val givenName: String,
    @SerialName("family_name")
    val familyName: String,
    val birthdate: String
) {
    override fun toString(): String {
        return "UserInfo(sub='$sub', givenName='$givenName', familyName='$familyName', birthdate='$birthdate')"
    }
}