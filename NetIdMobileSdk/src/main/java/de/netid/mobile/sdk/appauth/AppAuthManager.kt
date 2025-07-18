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
import de.netid.mobile.sdk.api.NetIdAuthFlow
import net.openid.appauth.AuthState

internal interface AppAuthManager {

    var listener: AppAuthManagerListener?

    /**
     * Fetches the discovery document which includes the configuration for the authentication endpoints.
     * During this call, it is checked if there is an AuthState present from a former session.
     * If so, this AuthState is used, otherwise a new one is created based on the service configuration.
     * @param host server address
     */
    fun fetchAuthorizationServiceConfiguration(host: String)

    /**
     * Starts the authorization process.
     * @param clientId the client id
     * @param redirectUri the uri to use as a callback
     * @param claims claims that should be set (for login flows), ignored for [NetIdAuthFlow.Permission]
     * @param prompt prompt value to be set for Web based flows, null otherwise
     * @param flow kind of flow, can be any of [NetIdAuthFlow.Permission], [NetIdAuthFlow.Login], or [NetIdAuthFlow.LoginPermission]
     * @param activity
     * @return intent
     */
    fun getAuthorizationIntent(
        clientId: String,
        redirectUri: String,
        claims: String?,
        prompt: String?,
        flow: NetIdAuthFlow,
        context: Context
    ): Intent?

    /**
     * Processes the authorization intent.
     * @param data the intent
     */
    fun processAuthorizationIntent(data: Intent)

    /**
     * Returns the currently available access token if there is one, null otherwise.
     * @return accessToken
     */
    fun getAccessToken(): String?

    fun setAccessToken(token: String?)

    /**
     * Returns the currently available permission token if there is one, null otherwise.
     * @return permissionToken
     */
    fun getPermissionToken(): String?

    /**
     * Returns the currently available auth state if there is one, null otherwise.
     * @return authState
     */
    fun getAuthState(): AuthState?

    /**
     * Ends the current session.
     */
    fun endSession()
}
