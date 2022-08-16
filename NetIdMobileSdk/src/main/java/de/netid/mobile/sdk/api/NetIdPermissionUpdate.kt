package de.netid.mobile.sdk.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetIdPermissionUpdate(
    @SerialName("idconsent")
    val idConsent: String,
    @SerialName("iab_tc_string")
    val iabTc: String
) {
    override fun toString(): String {
        return "NetIdPermissionUpdate(idConsent='$idConsent', iabTc='$iabTc')"
    }
}
