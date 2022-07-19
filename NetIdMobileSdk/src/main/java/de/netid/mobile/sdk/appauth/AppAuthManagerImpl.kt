package de.netid.mobile.sdk.appauth

import android.app.Activity
import android.content.Context
import android.net.Uri
import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.api.NetIdErrorCode
import de.netid.mobile.sdk.api.NetIdErrorProcess
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

class AppAuthManagerImpl : AppAuthManager {

    companion object {
        private const val scheme = "https://"
        private const val authRequestCode = 11 // TODO Different request code?
    }

    override var listener: AppAuthManagerListener? = null

    private var authorizationServiceConfiguration: AuthorizationServiceConfiguration? = null
    private var authState: AuthState? = null

    override fun fetchAuthorizationServiceConfiguration(host: String) {
        val uriString = scheme + host
        AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(uriString)) { serviceConfiguration, authorizationException ->
            authorizationException?.let {
                val netIdError: NetIdError = when (it) {
                    AuthorizationException.GeneralErrors.NETWORK_ERROR -> {
                        NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.NetworkError)
                    }
                    AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR -> {
                        NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.JsonDeserializationError)
                    }
                    AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT -> {
                        NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.InvalidDiscoveryDocument)
                    }
                    else -> {
                        NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.Unknown)
                    }
                }
                listener?.onAuthorizationServiceConfigurationFetchFailed(netIdError)
            } ?: run {
                serviceConfiguration?.let {
                    authorizationServiceConfiguration = serviceConfiguration
                    authState = AuthState(serviceConfiguration)

                    listener?.onAuthorizationServiceConfigurationFetchedSuccessfully()
                } ?: run {
                    val netIdError = NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.Unknown)
                    listener?.onAuthorizationServiceConfigurationFetchFailed(netIdError)
                }
            }
        }
    }

    override fun performWebAuthorization(clientId: String, redirectUri: String, activity: Activity) {
        authorizationServiceConfiguration?.let { serviceConfiguration ->
            val authRequestBuilder =
                AuthorizationRequest.Builder(serviceConfiguration, clientId, ResponseTypeValues.CODE, Uri.parse(redirectUri))
            val authRequest = authRequestBuilder.build()

            val authService = AuthorizationService(activity)
            val authIntent = authService.getAuthorizationRequestIntent(authRequest)
            activity.startActivityForResult(authIntent, authRequestCode)
        } ?: run {
            // TODO Error: No service configuration
        }
    }
}
