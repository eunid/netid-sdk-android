package de.netid.mobile.sdk.appauth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.api.NetIdErrorCode
import de.netid.mobile.sdk.api.NetIdErrorProcess
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
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
                val netIdError = createNetIdErrorForAuthorizationException(it)
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
            Log.e(javaClass.simpleName, "No authorization service configuration available")
            val netIdError = NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.UnauthorizedClient)
            listener?.onAuthorizationFailed(netIdError)
        }
    }

    override fun processAuthorizationIntent(requestCode: Int, data: Intent) {
        if (requestCode == authRequestCode) {
            val authorizationResponse = AuthorizationResponse.fromIntent(data)
            val authorizationException = AuthorizationException.fromIntent(data)

            authState?.update(authorizationResponse, authorizationException)

            authorizationException?.let {
                val netIdError = createNetIdErrorForAuthorizationException(it)
                listener?.onAuthorizationFailed(netIdError)
            } ?: run {
                authorizationResponse?.let {
                    // TODO Provide access token
                    listener?.onAuthorizationSuccessful()
                } ?: run {
                    val netIdError = NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.Unknown)
                    listener?.onAuthorizationServiceConfigurationFetchFailed(netIdError)
                }
            }
        } else {
            Log.i(javaClass.simpleName, "Request code does not match authorization request code")
        }
    }

    private fun createNetIdErrorForAuthorizationException(authorizationException: AuthorizationException): NetIdError {
        return when (authorizationException) {
            AuthorizationException.GeneralErrors.NETWORK_ERROR -> {
                NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.NetworkError)
            }
            AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR -> {
                NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.JsonDeserializationError)
            }
            AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT -> {
                NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.InvalidDiscoveryDocument)
            }
            AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.InvalidRequest)
            }
            AuthorizationException.AuthorizationRequestErrors.UNAUTHORIZED_CLIENT -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.UnauthorizedClient)
            }
            AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.AccessDenied)
            }
            AuthorizationException.AuthorizationRequestErrors.UNSUPPORTED_RESPONSE_TYPE -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.UnsupportedResponseType)
            }
            AuthorizationException.AuthorizationRequestErrors.INVALID_SCOPE -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.InvalidScope)
            }
            AuthorizationException.AuthorizationRequestErrors.SERVER_ERROR -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.ServerError)
            }
            AuthorizationException.AuthorizationRequestErrors.TEMPORARILY_UNAVAILABLE -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.TemporarilyUnavailable)
            }
            AuthorizationException.AuthorizationRequestErrors.CLIENT_ERROR -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.ClientError)
            }
            AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.StateMismatch)
            }
            AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.MissingBrowser)
            }
            AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW -> {
                NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.AuthorizationCanceledByUser)
            }
            else -> {
                NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.Unknown)
            }
        }
    }
}
