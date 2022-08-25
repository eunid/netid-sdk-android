package de.netid.mobile.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShippingAddress(
    @SerialName("street_address")
    val streetAddress: String,
    val country: String,
    val formatted: String,
    val locality: String,
    val recipient: String,
    val postal_code: String,

    ) {
    override fun toString(): String {
        return "ShippingAddress(streetAddress='$streetAddress', country='$country', formatted='$formatted', locality='$locality', recipient='$recipient', postal_code='$postal_code')"
    }
}