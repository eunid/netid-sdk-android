package de.netid.mobile.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectIdentifiers(
    @SerialName("tpid")
    val tpId: String,
    @SerialName("sync_id")
    val syncId: String = ""
) {
    override fun toString(): String {
        return "SubjectIdentifiers(tpId='$tpId', syncId='$syncId')"
    }
}
