package de.netid.mobile.sdk.appauth

interface AppAuthManagerListener {

    fun onAuthorizationServiceConfigurationFetchedSuccessfully()

    fun onAuthorizationServiceConfigurationFetchFailed()
}
