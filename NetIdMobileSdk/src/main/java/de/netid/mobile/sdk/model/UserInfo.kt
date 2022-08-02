package de.netid.mobile.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val sub: String,
    val given_name: String,
    val family_name: String,
    val birthdate: String
) {
    override fun toString(): String {
        return "UserInfo(sub='$sub', given_name='$given_name', family_name='$family_name', birthdate='$birthdate')"
    }
}