package de.netid.mobile.sdk.appauth

import de.netid.mobile.sdk.api.NetIdError

interface AppAuthManagerListener {

    fun onAuthorizationServiceConfigurationFetchedSuccessfully()

    fun onAuthorizationServiceConfigurationFetchFailed(error: NetIdError)
}
