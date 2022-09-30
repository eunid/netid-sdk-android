// Copyright 2022 European netID Foundation (https://enid.foundation)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.netid.mobile.sdk.appauth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import de.netid.mobile.sdk.api.NetIdAuthFlow
import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.api.NetIdErrorCode
import de.netid.mobile.sdk.api.NetIdErrorProcess
import de.netid.mobile.sdk.util.TokenUtil
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
        private const val scopePermissionManagement = "permission_management"
    }

    override var listener: AppAuthManagerListener? = null

    private var authorizationServiceConfiguration: AuthorizationServiceConfiguration? = null
    private var authState: AuthState? = null
    private var authService: AuthorizationService? = null
    private var idToken: String? = null

    override fun getAccessToken(): String? {
        return authState?.accessToken
    }

    override fun getIdToken(): String? {
        return idToken
    }

    override fun setIdToken(token: String) {
        idToken = token
    }

    override fun getPermissionToken(): String? {
        return getIdToken()?.let { TokenUtil.getPermissionTokenFrom(it) }
    }

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

    override fun getWebAuthorizationIntent(
        clientId: String,
        redirectUri: String,
        claims: Map<String, String>?,
        flow: NetIdAuthFlow,
        activity: Activity
    ): Intent? {
        authorizationServiceConfiguration?.let { serviceConfiguration ->
            var scopes = listOf<String>(scopePermissionManagement)
            when (flow) {
                NetIdAuthFlow.Hard -> {
                    scopes.plus(AuthorizationRequest.Scope.OPENID)
                    scopes.plus(AuthorizationRequest.Scope.PROFILE)
                }
                NetIdAuthFlow.Soft -> {
                }
            }

            val authRequestBuilder =
                AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    ResponseTypeValues.CODE,
                    Uri.parse(redirectUri)
                ).setScopes(scopes
                ).setAdditionalParameters(claims)
            val authRequest = authRequestBuilder.build()

            authService = AuthorizationService(activity)
            return authService?.getAuthorizationRequestIntent(authRequest)
        } ?: run {
            Log.e(javaClass.simpleName, "No authorization service configuration available")
            return null
        }
    }

    override fun processAuthorizationIntent(data: Intent) {
        val authorizationResponse = AuthorizationResponse.fromIntent(data)
        val authorizationException = AuthorizationException.fromIntent(data)

        authState?.update(authorizationResponse, authorizationException)

        authorizationException?.let {
            val netIdError = createNetIdErrorForAuthorizationException(it)
            listener?.onAuthorizationFailed(netIdError)
        } ?: run {
            authorizationResponse?.let {
                processTokenExchange(it)

            } ?: run {
                val netIdError =
                    NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.Unknown)
                listener?.onAuthorizationServiceConfigurationFetchFailed(netIdError)
            }
        }
    }

    private fun processTokenExchange(authorizationResponse: AuthorizationResponse) {
        authService?.performTokenRequest(authorizationResponse.createTokenExchangeRequest()) { response, exception ->
            authState?.update(response, exception)
            exception?.let { authException ->
                listener?.onAuthorizationFailed(createNetIdErrorForAuthorizationException(authException))
            } ?: run {
                response?.let { tokenResponse ->
                    Log.i(javaClass.simpleName, "Received token response: ${tokenResponse.accessToken}")
                    idToken = tokenResponse.idToken
                    listener?.onAuthorizationSuccessful()
                }
            }
        }
    }

    private fun createNetIdErrorForAuthorizationException(authorizationException: AuthorizationException): NetIdError {
        return when (authorizationException) {
            AuthorizationException.GeneralErrors.NETWORK_ERROR -> NetIdError(
                NetIdErrorProcess.Configuration,
                NetIdErrorCode.NetworkError
            )
            AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR ->
                NetIdError(
                    NetIdErrorProcess.Configuration,
                    NetIdErrorCode.JsonDeserializationError

            )
            AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT ->
                NetIdError(
                    NetIdErrorProcess.Configuration,
                    NetIdErrorCode.InvalidDiscoveryDocument

            )
            AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.InvalidRequest
            )
            AuthorizationException.AuthorizationRequestErrors.UNAUTHORIZED_CLIENT -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.UnauthorizedClient
            )
            AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.AccessDenied
            )
            AuthorizationException.AuthorizationRequestErrors.UNSUPPORTED_RESPONSE_TYPE -> NetIdError(

                    NetIdErrorProcess.Authentication,
                    NetIdErrorCode.UnsupportedResponseType

            )
            AuthorizationException.AuthorizationRequestErrors.INVALID_SCOPE -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.InvalidScope
            )
            AuthorizationException.AuthorizationRequestErrors.SERVER_ERROR -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.ServerError
            )
            AuthorizationException.AuthorizationRequestErrors.TEMPORARILY_UNAVAILABLE -> NetIdError(

                    NetIdErrorProcess.Authentication,
                    NetIdErrorCode.TemporarilyUnavailable

            )
            AuthorizationException.AuthorizationRequestErrors.CLIENT_ERROR -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.ClientError
            )
            AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.StateMismatch
            )
            AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.MissingBrowser
            )
            AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.AuthorizationCanceledByUser
            )
            else -> NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.Unknown)
        }
    }
}
