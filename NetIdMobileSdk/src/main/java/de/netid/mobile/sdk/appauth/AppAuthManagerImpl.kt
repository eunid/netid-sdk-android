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

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock

internal class AppAuthManagerImpl(context: Context) : AppAuthManager {

    companion object {
        private const val scheme = "https://"
        private const val scopePermissionManagement = "permission_management"
        private val browserDenyList = BrowserDenyList(
            VersionedBrowserMatcher.FIREFOX_BROWSER,
            VersionedBrowserMatcher.FIREFOX_CUSTOM_TAB
        )
        private const val STORE_NAME = "netIdSdk"
        private const val KEY_STATE = "authState"
        private val reentrantLock = ReentrantLock()
    }

    override var listener: AppAuthManagerListener? = null

    private var authorizationServiceConfiguration: AuthorizationServiceConfiguration? = null
    private var authService: AuthorizationService? = null
    private var authState: AuthState? = null
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    override fun getAccessToken(): String? {
        return getAuthState()?.accessToken
    }

    override fun getPermissionToken(): String? {
        // Fallback for getting a permission token as long as there is no refresh token flow (and only permission scope was requested).
        val token = getAuthState()?.idToken ?: return getAccessToken()
        return TokenUtil.getPermissionTokenFrom(token)
    }

    override fun getAuthState(): AuthState? {
        if (authState != null) {
            return authState
        }

        authState = readState()
        return authState
    }

    override fun setAccessToken(token: String?) {
        getAuthState()?.lastTokenResponse?.accessToken = token
    }

    /**
     * Read auth state from shared preferences if available.
     * If there is no state or if the state can not be reconstructed, null is returned.
     * @return the read auth state or null if there is none
     */
    private fun readState(): AuthState? {
        reentrantLock.lock()
        return try {
            sharedPreferences.getString(KEY_STATE, null)?.let { savedAuthState ->
                try {
                    AuthState.jsonDeserialize(savedAuthState)
                } catch (ex: JSONException) {
                    null
                }
            }?: run {
                null
            }
        } finally {
            reentrantLock.unlock()
        }
    }

    /**
     * Write current auth state to shared preferences.
     * If the given state is null, remove the currently stored state.
     * @param authState the state to persist or null to remove from store
     */
    private fun writeState(authState: AuthState?) {
        reentrantLock.lock()
        try {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            if (authState == null) {
                // Remove state from shared preferences
                editor.remove(KEY_STATE)
            } else {
                // Persist state in shared preferences
                editor.putString(KEY_STATE, authState.jsonSerializeString())
            }
            check(editor.commit()) {
                Log.e(javaClass.simpleName, "Failed to write auth state to shared preferences")
            }
        } finally {
            reentrantLock.unlock()
        }
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
                    authState = readState()
                    if (authState == null) {
                        authState = AuthState(serviceConfiguration)
                        writeState(authState)
                    }

                    listener?.onAuthorizationServiceConfigurationFetchedSuccessfully()
                } ?: run {
                    val netIdError = NetIdError(NetIdErrorProcess.Configuration, NetIdErrorCode.Unknown)
                    listener?.onAuthorizationServiceConfigurationFetchFailed(netIdError)
                }
            }
        }
    }

    override fun getAuthorizationIntent(
        clientId: String,
        redirectUri: String,
        claims: String?,
        prompt: String?,
        flow: NetIdAuthFlow,
        context: Context
    ): Intent? {
        authorizationServiceConfiguration?.let { serviceConfiguration ->
            val scopes = mutableListOf<String>()
            var claimsJSON: JSONObject? = claims?.let { JSONObject(claims) }

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
                ).setClaims(claimsJSON
                ).setPrompt(prompt)

            val authRequest = authRequestBuilder.build()
            val appAuthConfiguration = AppAuthConfiguration.Builder()
            .setBrowserMatcher(browserDenyList)
            .build()
            authService = AuthorizationService(context, appAuthConfiguration)

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
        writeState(authState)

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

    /**
     * Triggers the token exchange process after an authorization response has been received.
     * @param authorizationResponse the authorization response
     */
    private fun processTokenExchange(authorizationResponse: AuthorizationResponse) {
        authService?.performTokenRequest(authorizationResponse.createTokenExchangeRequest()) { response, exception ->
            if (authState == null) {
                listener?.onAuthorizationFailed(
                    NetIdError(NetIdErrorProcess.CodeExchange, NetIdErrorCode.InvalidAuthState)
                )
                return@performTokenRequest
            }
            authState?.update(response, exception)
            writeState(authState)
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

    /**
     * Internal function to convert an authorization exception to a NetIdError
     * @param process an id identifying the type of process that caused the error
     * @param authorizationException the actual authorization exception
     * @return NetIdError
     */
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

    override fun endSession() {
        //Initialize authState with existing AuthorizationServerConfiguration
        authorizationServiceConfiguration?.let {
            authState = AuthState(it)
        } ?: run {
            // Set to null in case AuthorizationService is not initialized
            authState = null
        }
        writeState(authState)
    }
}
