package de.netid.mobile.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class AppIdentifier(
    val id: Int,
    val name: String,
    val backgroundColor: String,
    val foregroundColor: String,
    val icon: String,
    val typeFaceIcon: String,
    val iOS: AppDetailsIOS,
    val android: AppDetailsAndroid,
)