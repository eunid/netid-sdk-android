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
import de.netid.mobile.sdk.ui.*
import de.netid.mobile.sdk.userinfo.UserInfoManager
import de.netid.mobile.sdk.userinfo.UserInfoManagerListener
import de.netid.mobile.sdk.util.JsonUtil
import de.netid.mobile.sdk.util.PackageUtil
import de.netid.mobile.sdk.util.ReachabilityUtil

/**
 * The ``NetIdService`` is the main class of the sdk.
 * An application communicates via this class with the authorization service.
 *
 * To do so, an application first registers itself as a listener to the service.
 * ``NetIdService.addListener(this)``
 *
 * Next, initialize the service with a configuration object of kind ``NetIdConfig``.
 *
 * The application has to conform to the ``NetIdServiceDelegate`` protocol and implement the required functions (see below).
 *
 * ``NetIdService.initialize(netIdConfig, this)``
 */
object NetIdService : AppAuthManagerListener, AuthorizationFragmentListener,
    UserInfoManagerListener, PermissionManagerListener {

    private const val appIdentifierFilename = "netIdAppIdentifiers.json"
    private const val broker = "broker.netid.de"
    private var layerStyle: NetIdLayerStyle = NetIdLayerStyle.Solid
    private var buttonStyle: NetIdButtonStyle = NetIdButtonStyle.WhiteSolid

    private var netIdConfig: NetIdConfig? = null

    private lateinit var appAuthManager: AppAuthManager
    private lateinit var userInfoManager: UserInfoManager
    private lateinit var permissionManager: PermissionManager

    private val availableAppIdentifiers = mutableListOf<AppIdentifier>()
    private val netIdServiceListeners = mutableSetOf<NetIdServiceListener>()

    private var permissionContinueButtonFragment:Fragment? = null
    private var loginContinueButtonFragment:Fragment? = null
    private var appButtonFragmentsForPermission = mutableMapOf<String, Fragment>()
    private var appButtonFragmentsForLogin = mutableMapOf<String, Fragment>()
    private var appButtonFragmentsForLoginPermission = mutableMapOf<String, Fragment>()

    /**
     * Registers a new listener of type NetIdServiceListener
     * @param listener The new listener.
     */
    fun addListener(listener: NetIdServiceListener) {
        netIdServiceListeners.add(listener)
    }

    /**
     * Removes a listener of type NetIdServiceListener from the list of registered listeners.
     * @param listener The listener to remove.
     */
    fun removeListener(listener: NetIdServiceListener) {
        netIdServiceListeners.remove(listener)
    }

    /**
     * Initializes the sdk and loads the authentication configuration document.
     * @param  netIdConfig The client configuration of type ``NetIdConfig``
     * @param  context Context to use.
     */
    fun initialize(netIdConfig: NetIdConfig, context: Context) {
        if (handleConnection(context, NetIdErrorProcess.Configuration)) {
            if (this.netIdConfig != null) {
                Log.w(javaClass.simpleName, "netId service configuration has been set already")
                return
            }

            this.netIdConfig = netIdConfig
            setupAuthManagerAndFetchConfiguration(context)
            setupUserInfoManager()
            setupPermissionManager()
            checkAvailableNetIdApplications(context)
        }
    }

    /**
     * Gets the authorization fragment for a requested authorization flow.
     * @param context Context to use.
     * @param authFlow Authorization flow to use, see ``NetIdAuthFlow``.
     * @param forceApp2App Set to true, if only app2app is allowed.
     * @return Fragment for authorization.
     */
    fun getAuthorizationFragment(context: Context, authFlow: NetIdAuthFlow, forceApp2App: Boolean = false): Fragment? {
        checkAvailableNetIdApplications(context)
        // If there are no ID apps installed, but forceApp2App is true, return with an error.
        if ((availableAppIdentifiers.isEmpty()) && forceApp2App) {
            this.onAuthorizationFailed(NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.NoIdAppInstalled))
            return null
        }

        netIdConfig?.let { config ->
            //prompt is applied only in App2Web Flows
            val effectivePrompt: String? =
                if(availableAppIdentifiers.isEmpty()) config.promptWeb else null

            return appAuthManager.getAuthorizationIntent(
                config.clientId,
                config.redirectUri,
                config.claims,
                effectivePrompt,
                authFlow,
                context
            )?.let {
                when (authFlow) {
                    NetIdAuthFlow.Login, NetIdAuthFlow.LoginPermission ->
                        AuthorizationLoginFragment(
                            this, availableAppIdentifiers, it, (config.loginLayerConfig?.headlineText) ?: "", (config.loginLayerConfig?.loginText) ?:"", (config.loginLayerConfig?.continueText)?: ""
                        )
                    NetIdAuthFlow.Permission ->
                        AuthorizationPermissionFragment(
                            this, availableAppIdentifiers, it, (config.permissionLayerConfig?.logoName)?: "", (config.permissionLayerConfig?.headlineText)?: "", (config.permissionLayerConfig?.legalText)?: "", (config.permissionLayerConfig?.continueText)?: ""
                        )
                }
            }
        }
        return null
    }

    /**
     * Sets the style to use for all layers when using the layer flow.
     * @param layerStyle button style to set, can be any of ``NetIdLayerStyle``, defaults to ``NetIdLayerStyle.Solid``
     */
    fun setLayerStyle(layerStyle: NetIdLayerStyle) {
        this.layerStyle = layerStyle
    }

    /**
     * Gets the currently set style that's used for all layers when using the layer flow.
     * @return Currently set style
     */
    fun getLayerStyle(): NetIdLayerStyle {
        return layerStyle
    }

    /**
     * Sets the style to use for all buttons when using the button flow.
     * @param buttonStyle button style to set, can be any of ``NetIdButtonStyle``, defaults to ``NetIdButtonStyle.GraySolid``
     */
    fun setButtonStyle(buttonStyle: NetIdButtonStyle) {
        this.buttonStyle = buttonStyle

        if (permissionContinueButtonFragment != null) {
            (permissionContinueButtonFragment as PermissionContinueButtonFragment).setButtonStyle(buttonStyle)
        }

        if (loginContinueButtonFragment != null) {
            (loginContinueButtonFragment as LoginContinueButtonFragment).setButtonStyle(buttonStyle)
        }

        appButtonFragmentsForPermission.forEach {
            val frag = it.value as AccountProviderAppButtonFragment
            frag.setButtonStyle(buttonStyle)
        }

        appButtonFragmentsForLogin.forEach {
            val frag = it.value as AccountProviderAppButtonFragment
            frag.setButtonStyle(buttonStyle)
        }

        appButtonFragmentsForLoginPermission.forEach {
            val frag = it.value as AccountProviderAppButtonFragment
            frag.setButtonStyle(buttonStyle)
        }
    }

    /**
     * Gets the currently set style that's used for all buttons when using the button flow.
     * @return Currently set style
     */
    fun getButtonStyle(): NetIdButtonStyle {
        return buttonStyle
    }

    /**
     * Returns the count of installed account provider apps.
     * Use this function only if you intent to build your very own authorization dialog.
     * @return: Count of installed account provider apps.
     */
    fun getCountOfAccountProviderApps(context: Context): Int {
        checkAvailableNetIdApplications(context)
        return availableAppIdentifiers.count()
    }

    /**
     * Returns the keys of installed account provider apps. With these keys, you can request buttons for specific account provider apps identified by their key aka name.
     * Use this function only if you intent to build your very own authorization dialog.
     * @return: Array of keys of installed account provider apps.
     */
    fun getKeysForAccountProviderApps(): Array<String> {
        val result = mutableListOf<String>()
        availableAppIdentifiers.forEach {
            result.add(it.name)
        }
        return result.toTypedArray()
    }

    /**
     * Returns the authorization intent for a requested flow.
     * @param flow Requested flow.
     * @param context Context to use.
     * @return Authorization intent.
     */
    internal fun authIntentForFlow(flow: NetIdAuthFlow, context: Context): Intent? {
        netIdConfig?.let { config ->
            return appAuthManager.getAuthorizationIntent(
                config.clientId,
                config.redirectUri,
                config.claims,
                config.promptWeb,
                flow,
                context
            )
        }
        return null
    }

    /**
     * Returns the continue button (as a fragment) in case of a permission flow dialog.
     * Use this function only if you intent to build your very own authorization dialog.
     * @param continueText Alternative text to set on the button. If empty, the default will be used.
     * @return Fragment for authorization.
     */
    fun permissionContinueButtonFragment(continueText: String = ""): Fragment {
        if (permissionContinueButtonFragment == null) {
            permissionContinueButtonFragment = PermissionContinueButtonFragment(this, continueText)
        }
        return permissionContinueButtonFragment as Fragment
    }

    /**
     * Returns the continue button (as a fragment) in case of a login flow dialog.
     * Use this function only if you intent to build your very own authorization dialog.
     * @param continueText Alternative text to set on the button. If empty, the default will be used.
     * @param authFlow Must either be .Login or .LoginPermission. If is set to .Permission, an error will be thrown.
     * @return Fragment for authorization.
     */
    fun loginContinueButtonFragment(continueText: String = "", flow: NetIdAuthFlow): Fragment {
        if (loginContinueButtonFragment == null) {
            loginContinueButtonFragment = LoginContinueButtonFragment(this, continueText, flow)
        }
        return loginContinueButtonFragment as Fragment
    }

    /**
     * Returns the button for a certain account provider app for a requested ``NetIdAuthFlow``
     * Use this function only if you intent to build your very own authorization dialog.
     * @param key Key denoting one of the installed account provider apps. Use ``getKeysForAccountProviderApps`` first to get the keys/names of all installed account provider apps.
     * @param authFlow Can be any of .Permission, .Login or .LoginPermission.
     * @param continueText Alternative text to set on the button. If empty, the default will be used.
     * @returns Button with text and label for the chosen id app. If index is out of bounds or no app is installed, ArrayIndexOutOfBoundsException is thrown.
     * @throws ArrayIndexOutOfBoundsException
     */
    fun accountProviderAppButtonFragment(key: String, flow: NetIdAuthFlow, continueText: String = ""): Fragment {
        val keys = getKeysForAccountProviderApps()
        val appButtonFragments = when (flow) {
            NetIdAuthFlow.Permission -> appButtonFragmentsForPermission
            NetIdAuthFlow.Login -> appButtonFragmentsForLogin
            NetIdAuthFlow.LoginPermission -> appButtonFragmentsForLoginPermission
        }
        if (keys.contains(key)) {
            return if (appButtonFragments.containsKey(key)) {
                appButtonFragments[key] as Fragment
            } else {
                val index = keys.binarySearch(key)
                val app = AccountProviderAppButtonFragment(this, availableAppIdentifiers[index], flow, continueText)
                appButtonFragments[key] = app
                app
            }
        }
        throw ArrayIndexOutOfBoundsException()
    }

    /**
     * Checks whether there is a network connection or not.
     * @param context Context to use.
     * @param process: In case of an error, denotes the process that the error is responsible for.
     * @returns bool
     */
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

    /**
     * Fetches the user info.
     * @param context Context to use.
     */
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

    /**
     * Fetch permissions.
     * @param context Context to use.
     * @param collapseSyncId: Boolean value to indicate whether syncId is used or not.
     */
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

    /**
     * Update permissions.
     * @param context Context to use.
     * @param permission Permissions to set, of type ``NetIdPermissionUpdate``.
     * @param collapseSyncId Boolean value to indicate if syncId is used or not.
     */
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

    fun setAccessToken(token: String?) {
        appAuthManager.setAccessToken(token)
    }

    /**
     * Function to end a session.
     * The net ID service itself still remains initialized but all information about authorization/authentication is discarded.
     * To start a new session, call ``authorize(destinationScheme:currentViewController:authFlow)`` again.
     */
    fun endSession() {
        Log.i(javaClass.simpleName, "netId service did end session successfully")
        appAuthManager.endSession()
        for (item in netIdServiceListeners) {
            item.onSessionEnd()
        }
    }

    private fun setupAuthManagerAndFetchConfiguration(context: Context) {
        appAuthManager = AppAuthManagerFactory.createAppAuthManager(context)
        appAuthManager.listener = this
        appAuthManager.fetchAuthorizationServiceConfiguration(broker)
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
