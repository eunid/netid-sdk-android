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
    val birthdate: String,
    @SerialName("email_verified")
    val emailVerified: Boolean = false,
    val address: Address? = null,
    @SerialName("shipping_address")
    val shippingAddress: ShippingAddress? = null,
    val gender: String = "",
    val email: String = "",
    ) {
    override fun toString(): String {
        return "UserInfo(sub='$sub', givenName='$givenName', familyName='$familyName', birthdate='$birthdate', emailVerified=$emailVerified, address=$address, shippingAddress=$shippingAddress, gender='$gender', email='$email')"
    }
}