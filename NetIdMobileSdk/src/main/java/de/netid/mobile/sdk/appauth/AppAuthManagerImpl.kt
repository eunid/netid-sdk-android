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
import net.openid.appauth.*
import org.json.JSONObject


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
    private var authRequest: AuthorizationRequest? = null

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
        // Fallback for getting a permission token as long as there is no refresh token flow (and only permission scope was requested).
        val token = getIdToken() ?: return getAccessToken()
        return TokenUtil.getPermissionTokenFrom(token)
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
        claims: String,
        flow: NetIdAuthFlow,
        activity: Activity
    ): Intent? {
        authorizationServiceConfiguration?.let { serviceConfiguration ->
            var scopes = mutableListOf<String>()
            when (flow) {
                NetIdAuthFlow.Login -> {
                    scopes.add(AuthorizationRequest.Scope.OPENID)
                }
                NetIdAuthFlow.LoginPermission -> {
                    scopes.add(AuthorizationRequest.Scope.OPENID)
                    scopes.add(scopePermissionManagement)
                }
                NetIdAuthFlow.Permission -> {
                    scopes.add(scopePermissionManagement)
                }
            }

            val claimsJSON = if(claims.isEmpty() ) null else JSONObject(claims)
            val authRequestBuilder =
                AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    ResponseTypeValues.CODE,
                    Uri.parse(redirectUri)
                ).setScopes(scopes
                ).setClaims(claimsJSON)
            authRequest = authRequestBuilder.build()

            authService = AuthorizationService(activity)
            return authService?.getAuthorizationRequestIntent(authRequest!!)
        } ?: run {
            Log.e(javaClass.simpleName, "No authorization service configuration available")
            return null
        }
    }

    override fun processAuthorizationIntent(data: Intent) {
        val authorizationResponse = AuthorizationResponse.Builder(authRequest!!).fromUri(data.data!!).build()
        var authorizationException = AuthorizationException.fromIntent(data)

        authorizationException?.let {
            val netIdError = createNetIdErrorForAuthorizationException(it)
            listener?.onAuthorizationFailed(netIdError)
        } ?: run {
            authorizationResponse.let {
                processTokenExchange(it)
            }
        }
    }

    private fun processTokenExchange(authorizationResponse: AuthorizationResponse) {
        authService?.performTokenRequest(authorizationResponse.createTokenExchangeRequest()) { response, exception ->
            authState?.update(response, exception)
            exception?.let { authException ->
                listener?.onAuthorizationFailed(
                    createNetIdErrorForAuthorizationException(
                        authException
                    )
                )
            } ?: run {
                response?.let { tokenResponse ->
                    Log.i(
                        javaClass.simpleName,
                        "Received token response: ${tokenResponse.accessToken}"
                    )
                    idToken = tokenResponse.idToken
                    listener?.onAuthorizationSuccessful()
                }
            }
        }
    }

    private fun createNetIdErrorForAuthorizationException(authorizationException: AuthorizationException): NetIdError {
        var msg = ""
        if (authorizationException.error != null) {
            msg = authorizationException.error + " - " + authorizationException.errorDescription
        }
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
            AuthorizationException.AuthorizationRequestErrors.OTHER -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.Other,
                msg
            )
            AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.MissingBrowser
            )
            AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.AuthorizationCanceledByUser
            )
            AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW -> NetIdError(
                NetIdErrorProcess.Authentication,
                NetIdErrorCode.AuthorizationCanceledByProgram
            )
            AuthorizationException.TokenRequestErrors.OTHER -> NetIdError(
                NetIdErrorProcess.CodeExchange,
                NetIdErrorCode.Other,
                msg
            )
            else -> NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.Unknown, msg)
        }
    }
}
