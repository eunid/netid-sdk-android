package de.netid.mobile.sdk.api

data class NetIdError(
    val process: NetIdErrorProcess,
    val code: NetIdErrorCode
)
