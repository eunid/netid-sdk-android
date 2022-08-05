package de.netid.mobile.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class AppDetailsIOS(val bundleIdentifier: String, val scheme: String)
