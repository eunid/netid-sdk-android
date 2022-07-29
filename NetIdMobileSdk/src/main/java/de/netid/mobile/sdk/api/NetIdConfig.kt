package de.netid.mobile.sdk.api

data class NetIdConfig(
    val host: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String
)
