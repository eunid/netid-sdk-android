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
import net.openid.appauth.browser.BrowserDenyList
import net.openid.appauth.browser.VersionedBrowserMatcher
import org.json.JSONObject


class AppAuthManagerImpl : AppAuthManager {

    companion object {
        private const val scheme = "https://"
        private const val scopePermissionManagement = "permission_management"
        private val browserDenyList =  BrowserDenyList(
            VersionedBrowserMatcher.FIREFOX_BROWSER,
            VersionedBrowserMatcher.FIREFOX_CUSTOM_TAB
        )
    }

    override var listener: AppAuthManagerListener? = null

    private var authorizationServiceConfiguration: AuthorizationServiceConfiguration? = null
    private var authState: AuthState? = null
    private var authService: AuthorizationService? = null

    override fun getAccessToken(): String? {
        return authState?.accessToken
    }

    override fun getPermissionToken(): String? {
        // Fallback for getting a permission token as long as there is no refresh token flow (and only permission scope was requested).
        val token = authState?.idToken ?: return getAccessToken()
        return TokenUtil.getPermissionTokenFrom(token)
    }

    override fun getAuthState(): AuthState? {
        return authState
    }

    override fun fetchAuthorizationServiceConfiguration(host: String) {
        val uriString = scheme + host
        AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(uriString)) { serviceConfiguration, authorizationException ->
            authorizationException?.let {
                val netIdError = createNetIdErrorForAuthorizationException(
                    NetIdErrorProcess.Configuration,
                    it
                )
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
            var claimsJSON: JSONObject? = if(claims.isEmpty()) null else JSONObject(claims)
            when (flow) {
                NetIdAuthFlow.Login -> {
                    scopes.add(AuthorizationRequest.Scope.OPENID)
                }
                NetIdAuthFlow.LoginPermission -> {
                    scopes.add(AuthorizationRequest.Scope.OPENID)
                    scopes.add(scopePermissionManagement)
                }
                NetIdAuthFlow.Permission -> {
                    // remove claims, not relevant for this flow
                    claimsJSON = null
                    scopes.add(scopePermissionManagement)
                }
            }
            val authRequestBuilder =
                AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    ResponseTypeValues.CODE,
                    Uri.parse(redirectUri)
                ).setScopes(scopes
                ).setClaims(claimsJSON)
            val authRequest = authRequestBuilder.build()
            val appAuthConfiguration = AppAuthConfiguration.Builder()
            .setBrowserMatcher(browserDenyList)
            .build()
            authService = AuthorizationService(activity, appAuthConfiguration)

            return authService?.getAuthorizationRequestIntent(authRequest)
        } ?: run {
            Log.e(javaClass.simpleName, "No authorization service configuration available")
            return null
        }
    }

    override fun processAuthorizationIntent(data: Intent) {
        val authorizationResponse = AuthorizationResponse.fromIntent(data)
        val authorizationException = AuthorizationException.fromIntent(data)

        authState?.update(authorizationResponse,authorizationException)

        authorizationException?.let {
            val netIdError = createNetIdErrorForAuthorizationException(
                NetIdErrorProcess.Authentication,
                it
            )
            listener?.onAuthorizationFailed(netIdError)
        } ?: run {
            authorizationResponse?.let {
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
                        NetIdErrorProcess.CodeExchange,
                        authException
                    )
                )
            } ?: run {
                response?.let { tokenResponse ->
                    Log.i(
                        javaClass.simpleName,
                        "Received token response: ${tokenResponse.accessToken}"
                    )
                    listener?.onAuthorizationSuccessful()
                }
            }
        }
    }

    private fun createNetIdErrorForAuthorizationException(process: NetIdErrorProcess, authorizationException: AuthorizationException): NetIdError {
        var msg = ""
        if (authorizationException.error != null) {
            msg = authorizationException.error + " - " + authorizationException.errorDescription
        }
        return when (authorizationException) {
            AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST -> NetIdError(
                process,
                NetIdErrorCode.InvalidRequest
            )
            AuthorizationException.AuthorizationRequestErrors.UNAUTHORIZED_CLIENT -> NetIdError(
                process,
                NetIdErrorCode.UnauthorizedClient
            )
            AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED -> NetIdError(
                process,
                NetIdErrorCode.AccessDenied
            )
            AuthorizationException.AuthorizationRequestErrors.UNSUPPORTED_RESPONSE_TYPE -> NetIdError(
                process,
                NetIdErrorCode.UnsupportedResponseType
            )
            AuthorizationException.AuthorizationRequestErrors.INVALID_SCOPE -> NetIdError(
                process,
                NetIdErrorCode.InvalidScope
            )
            AuthorizationException.AuthorizationRequestErrors.SERVER_ERROR -> NetIdError(
                process,
                NetIdErrorCode.ServerError
            )
            AuthorizationException.AuthorizationRequestErrors.TEMPORARILY_UNAVAILABLE -> NetIdError(
                process,
                NetIdErrorCode.TemporarilyUnavailable
            )
            AuthorizationException.AuthorizationRequestErrors.CLIENT_ERROR -> NetIdError(
                process,
                NetIdErrorCode.ClientError
            )
            AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH -> NetIdError(
                process,
                NetIdErrorCode.StateMismatch
            )
            AuthorizationException.AuthorizationRequestErrors.OTHER -> NetIdError(
                process,
                NetIdErrorCode.Other,
                msg
            )
            AuthorizationException.GeneralErrors.NETWORK_ERROR -> NetIdError(
                process,
                NetIdErrorCode.NetworkError
            )
            AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR -> NetIdError(
                process,
                NetIdErrorCode.JsonDeserializationError
            )
            AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT -> NetIdError(
                process,
                NetIdErrorCode.InvalidDiscoveryDocument
            )
            AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW -> NetIdError(
                process,
                NetIdErrorCode.MissingBrowser
            )
            AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW -> NetIdError(
                process,
                NetIdErrorCode.AuthorizationCanceledByUser
            )
            AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW -> NetIdError(
                process,
                NetIdErrorCode.AuthorizationCanceledByProgram
            )
            AuthorizationException.TokenRequestErrors.CLIENT_ERROR -> NetIdError(
                process,
                NetIdErrorCode.ClientError
            )
            AuthorizationException.TokenRequestErrors.INVALID_CLIENT -> NetIdError(
                process,
                NetIdErrorCode.InvalidClient
            )
            AuthorizationException.TokenRequestErrors.INVALID_GRANT -> NetIdError(
                process,
                NetIdErrorCode.InvalidGrant
            )
            AuthorizationException.TokenRequestErrors.INVALID_REQUEST -> NetIdError(
                process,
                NetIdErrorCode.InvalidRequest
            )
            AuthorizationException.TokenRequestErrors.INVALID_SCOPE -> NetIdError(
                process,
                NetIdErrorCode.InvalidScope
            )
            AuthorizationException.TokenRequestErrors.UNAUTHORIZED_CLIENT -> NetIdError(
                process,
                NetIdErrorCode.UnauthorizedClient
            )
            AuthorizationException.TokenRequestErrors.UNSUPPORTED_GRANT_TYPE -> NetIdError(
                process,
                NetIdErrorCode.UnsupportedGrantType
            )
            AuthorizationException.TokenRequestErrors.OTHER -> NetIdError(
                process,
                NetIdErrorCode.Other,
                msg
            )
            else -> NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.Unknown, msg)
        }
    }
}
