package de.netid.mobile.sdk.appauth

interface AppAuthManager {

    var listener: AppAuthManagerListener?

    fun fetchAuthorizationServiceConfiguration(host: String)
}
