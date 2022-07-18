package de.netid.mobile.sdk.api

import java.util.UUID

data class NetIdConfig(
    val host: String,
    val clientId: UUID,
    val clientSecret: UUID,
    val redirectUri: String
)
