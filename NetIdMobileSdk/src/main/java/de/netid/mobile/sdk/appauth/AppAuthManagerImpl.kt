package de.netid.mobile.sdk.appauth

import android.net.Uri
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationServiceConfiguration

class AppAuthManagerImpl: AppAuthManager {

    override var listener: AppAuthManagerListener? = null

    private var authorizationServiceConfiguration: AuthorizationServiceConfiguration? = null
    private var authState: AuthState? = null

    override fun fetchAuthorizationServiceConfiguration(uriString: String) {
        AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(uriString)) { serviceConfiguration, authorizationException ->
            authorizationException?.let {
                listener?.onAuthorizationServiceConfigurationFetchFailed()
            } ?: run {
                serviceConfiguration?.let {
                    authorizationServiceConfiguration = serviceConfiguration
                    authState = AuthState(serviceConfiguration)

                    listener?.onAuthorizationServiceConfigurationFetchedSuccessfully()
                } ?: run {
                    val error = AuthorizationException.AuthorizationRequestErrors.OTHER
                    listener?.onAuthorizationServiceConfigurationFetchFailed()
                }
            }
        }
    }
}
