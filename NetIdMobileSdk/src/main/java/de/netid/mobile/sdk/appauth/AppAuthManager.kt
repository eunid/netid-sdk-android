package de.netid.mobile.sdk.appauth

interface AppAuthManager {

    var listener: AppAuthManagerListener?

    fun fetchAuthorizationServiceConfiguration(uriString: String)
}
