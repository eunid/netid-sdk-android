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

package de.netid.mobile.sdk.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.appauth.AppAuthManager
import de.netid.mobile.sdk.appauth.AppAuthManagerFactory
import de.netid.mobile.sdk.appauth.AppAuthManagerListener
import de.netid.mobile.sdk.model.*
import de.netid.mobile.sdk.permission.PermissionManager
import de.netid.mobile.sdk.permission.PermissionManagerListener
import de.netid.mobile.sdk.ui.AuthorizationFragmentListener
import de.netid.mobile.sdk.ui.AuthorizationLoginFragment
import de.netid.mobile.sdk.ui.AuthorizationPermissionFragment
import de.netid.mobile.sdk.userinfo.UserInfoManager
import de.netid.mobile.sdk.userinfo.UserInfoManagerListener
import de.netid.mobile.sdk.util.JsonUtil
import de.netid.mobile.sdk.util.PackageUtil
import de.netid.mobile.sdk.util.ReachabilityUtil

object NetIdService : AppAuthManagerListener, AuthorizationFragmentListener,
    UserInfoManagerListener, PermissionManagerListener {

    private const val appIdentifierFilename = "netIdAppIdentifiers.json"
    private const val broker = "broker.netid.de"

    private var netIdConfig: NetIdConfig? = null

    private lateinit var appAuthManager: AppAuthManager
    private lateinit var userInfoManager: UserInfoManager
    private lateinit var permissionManager: PermissionManager

    private val availableAppIdentifiers = mutableListOf<AppIdentifier>()
    private val netIdServiceListeners = mutableSetOf<NetIdServiceListener>()

    fun addListener(listener: NetIdServiceListener) {
        netIdServiceListeners.add(listener)
    }

    fun removeListener(listener: NetIdServiceListener) {
        netIdServiceListeners.remove(listener)
    }

    fun initialize(netIdConfig: NetIdConfig, context: Context) {
        if (handleConnection(context, NetIdErrorProcess.Configuration)) {
            if (this.netIdConfig != null) {
                Log.w(javaClass.simpleName, "netId service configuration has been set already")
                return
            }

            this.netIdConfig = netIdConfig
            setupAuthManagerAndFetchConfiguration(context, broker)
            setupUserInfoManager()
            setupPermissionManager()
        }
    }

    fun getAuthorizationFragment(activity: Activity, authFlow: NetIdAuthFlow, forceApp2App: Boolean = false): Fragment? {
        checkAvailableNetIdApplications(activity)
        // If there are no ID apps installed, but forceApp2App is true, return with an error.
        if ((availableAppIdentifiers.isEmpty()) && forceApp2App) {
            this.onAuthorizationFailed(NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.NoIdAppInstalled))
            return null
        }

        netIdConfig?.let { config ->
            //prompt is applied only in App2Web Flows
            val effectivePrompt: String? =
                if(availableAppIdentifiers.isEmpty()) config.promptWeb else null

            return appAuthManager.getWebAuthorizationIntent(
                config.clientId,
                config.redirectUri,
                config.claims,
                effectivePrompt,
                authFlow,
                activity
            )?.let {
                when (authFlow) {
                    NetIdAuthFlow.Login, NetIdAuthFlow.LoginPermission ->
                        AuthorizationLoginFragment(
                            this, availableAppIdentifiers, it, (config.loginLayerConfig?.headlineText) ?: "", (config.loginLayerConfig?.loginText) ?:"", (config.loginLayerConfig?.continueText)?: ""
                        )
                    NetIdAuthFlow.Permission ->
                        AuthorizationPermissionFragment(
                            this, availableAppIdentifiers, it, (config.permissionLayerConfig?.logoId)?: "", (config.permissionLayerConfig?.headlineText)?: "", (config.permissionLayerConfig?.legalText)?: "", (config.permissionLayerConfig?.continueText)?: ""
                        )
                }
            }
        }
        return null
    }

    private fun handleConnection(context: Context, process: NetIdErrorProcess): Boolean {
        return if (ReachabilityUtil.hasConnection(context)) {
            true
        } else {
            for (item in netIdServiceListeners) {
                item.onEncounteredNetworkError(NetIdError(process, NetIdErrorCode.NetworkError))
            }
            false
        }
    }

    fun fetchUserInfo(context: Context) {
        if (handleConnection(context, NetIdErrorProcess.UserInfo)) {
            var error: NetIdError? = null

            appAuthManager.getAccessToken()?.let { token ->
                appAuthManager.getAuthState()?.authorizationServiceConfiguration?.discoveryDoc?.userinfoEndpoint?.let{ endpoint ->
                    userInfoManager.fetchUserInfo(
                        endpoint,
                        token)
                } ?:{
                    error = NetIdError(NetIdErrorProcess.UserInfo, NetIdErrorCode.InvalidDiscoveryDocument)
                }            } ?: run {
                error = NetIdError(NetIdErrorProcess.UserInfo, NetIdErrorCode.UnauthorizedClient)
            }

            error?.let {
                for (item in netIdServiceListeners) {
                    item.onUserInfoFetchedWithError(it)
                }
            }
        }
    }

    fun fetchPermissions(context: Context, collapseSyncId: Boolean = true) {
        if (handleConnection(context, NetIdErrorProcess.PermissionRead)) {
            var error: NetIdError? = null
            appAuthManager.getPermissionToken()?.let { token ->
                permissionManager.fetchPermissions(token, collapseSyncId)
            } ?: run {
                error = NetIdError(NetIdErrorProcess.PermissionRead, NetIdErrorCode.UnauthorizedClient)
            }
            error?.let {
                for (item in netIdServiceListeners) {
                    item.onPermissionFetchFinishedWithError(PermissionResponseStatus.UNKNOWN, it)
                }
            }
        }
    }

    fun updatePermission(context: Context, permission: NetIdPermissionUpdate, collapseSyncId: Boolean = true) {
        if (handleConnection(context, NetIdErrorProcess.PermissionWrite)) {
            var error: NetIdError? = null
            appAuthManager.getPermissionToken()?.let { token ->
                permissionManager.updatePermission(token, permission, collapseSyncId)
            } ?: run {
                error = NetIdError(NetIdErrorProcess.PermissionWrite, NetIdErrorCode.UnauthorizedClient)
            }
            error?.let {
                for (item in netIdServiceListeners) {
                    item.onPermissionUpdateFinishedWithError(PermissionResponseStatus.UNKNOWN, it)
                }
            }

        }
    }

    private fun setupAuthManagerAndFetchConfiguration(context: Context, host: String) {
        appAuthManager = AppAuthManagerFactory.createAppAuthManager(context)
        appAuthManager.listener = this
        appAuthManager.fetchAuthorizationServiceConfiguration(host)
    }

    private fun setupUserInfoManager() {
        userInfoManager = UserInfoManager(this)
    }

    private fun setupPermissionManager() {
        permissionManager = PermissionManager(this)
    }

    private fun checkAvailableNetIdApplications(context: Context) {
        availableAppIdentifiers.clear()
        val appIdentifiers = JsonUtil.loadAppIdentifiers(appIdentifierFilename, context)
        val installedAppIdentifiers = PackageUtil.getInstalledPackages(appIdentifiers, context.packageManager)
        availableAppIdentifiers.addAll(installedAppIdentifiers)
    }

    fun endSession() {
        Log.i(javaClass.simpleName, "netId service did end session successfully")
        appAuthManager.endSession()
    }

// AppAuthManagerListener functions

    override fun onAuthorizationServiceConfigurationFetchedSuccessfully() {
        Log.i(javaClass.simpleName, "netId service Authorization Service Configuration fetched successfully")
        for (item in netIdServiceListeners) {
            item.onInitializationFinishedWithError(null)
        }
        // Do we have (already) a session (maybe from last time)? Then try to make use of it.
        if (appAuthManager.getAuthState() != null) {
            onAuthorizationSuccessful()
        }
    }

    override fun onAuthorizationServiceConfigurationFetchFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "netId service Authorization Service Configuration fetch failed")
        for (item in netIdServiceListeners) {
            item.onInitializationFinishedWithError(error)
        }
    }

    override fun onAuthorizationSuccessful() {
        Log.i(javaClass.simpleName, "netId service Authorization successful")
        appAuthManager.getAccessToken()?.let {
            for (item in netIdServiceListeners) {
                item.onAuthenticationFinished(it)
            }
        }
    }

    override fun onAuthorizationFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "netId service Authorization failed")
        for (item in netIdServiceListeners) {
            item.onAuthenticationFinishedWithError(error)
        }
    }

// AuthorizationFragmentListener functions

    override fun onAuthenticationFinished(response: Intent?) {
        response?.let { intent ->
            appAuthManager.processAuthorizationIntent(intent)
        } ?: run {
            for (item in netIdServiceListeners) {
                item.onAuthenticationFinishedWithError(
                    NetIdError(
                        NetIdErrorProcess.Authentication,
                        NetIdErrorCode.Unknown
                    )
                )
            }
        }
    }

    override fun onAuthenticationFailed() {
        for (item in netIdServiceListeners) {
            item.onAuthenticationCanceled(
                NetIdError(
                    NetIdErrorProcess.Authentication,
                    NetIdErrorCode.AuthorizationCanceledByUser
                )
            )
        }
    }

    override fun onCloseClicked() {
        Log.i(javaClass.simpleName, "netId service close authentication")
        for (item in netIdServiceListeners) {
            item.onAuthenticationCanceled(
                NetIdError(
                    NetIdErrorProcess.Authentication,
                    NetIdErrorCode.AuthorizationCanceledByUser
                )
            )
        }
    }

    override fun onAppButtonClicked(appIdentifier: AppIdentifier) {
        Log.i(javaClass.simpleName, "netId service will use app ${appIdentifier.name} for authentication")
    }

// UserInfoManagerListener functions

    override fun onUserInfoFetched(userInfo: UserInfo) {
        Log.i(javaClass.simpleName, "netId service user info fetched successfully")
        for (item in netIdServiceListeners) {
            item.onUserInfoFinished(userInfo)
        }
    }

    override fun onUserInfoFetchFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "netId service user info fetch failed")
        for (item in netIdServiceListeners) {
            item.onUserInfoFetchedWithError(error)
        }
    }

    // PermissionManagerListener functions
    override fun onPermissionsFetched(permissions: PermissionReadResponse) {
        Log.i(javaClass.simpleName, "netId service permissions fetched successfully")
        for (item in netIdServiceListeners) {
            item.onPermissionFetchFinished(permissions)
        }
    }

    override fun onPermissionsFetchFailed(statusCode: PermissionResponseStatus, error: NetIdError) {
        Log.e(javaClass.simpleName, "netId service permissions fetch failed")
        for (item in netIdServiceListeners) {
            item.onPermissionFetchFinishedWithError(statusCode, error)
        }
    }

    override fun onPermissionUpdated(subjectIdentifiers: SubjectIdentifiers) {
        Log.i(javaClass.simpleName, "netId service permissions updated successfully")
        for (item in netIdServiceListeners) {
            item.onPermissionUpdateFinished(subjectIdentifiers)
        }
    }

    override fun onPermissionUpdateFailed(statusCode: PermissionResponseStatus, error: NetIdError) {
        Log.e(javaClass.simpleName, "netId service permission update failed")
        for (item in netIdServiceListeners) {
            item.onPermissionUpdateFinishedWithError(statusCode, error)
        }
    }
}
