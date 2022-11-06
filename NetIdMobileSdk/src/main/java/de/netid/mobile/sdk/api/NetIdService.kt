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
import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.Log
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.appauth.AppAuthManager
import de.netid.mobile.sdk.appauth.AppAuthManagerFactory
import de.netid.mobile.sdk.appauth.AppAuthManagerListener
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.model.Permissions
import de.netid.mobile.sdk.model.SubjectIdentifiers
import de.netid.mobile.sdk.model.UserInfo
import de.netid.mobile.sdk.permission.PermissionManager
import de.netid.mobile.sdk.permission.PermissionManagerListener
import de.netid.mobile.sdk.ui.AuthorizationFragmentListener
import de.netid.mobile.sdk.ui.AuthorizationHardFragment
import de.netid.mobile.sdk.ui.AuthorizationSoftFragment
import de.netid.mobile.sdk.userinfo.UserInfoManager
import de.netid.mobile.sdk.userinfo.UserInfoManagerListener
import de.netid.mobile.sdk.util.JsonUtil
import de.netid.mobile.sdk.util.PackageUtil
import de.netid.mobile.sdk.util.ReachabilityUtil
import de.netid.mobile.sdk.util.TokenUtil

object NetIdService : AppAuthManagerListener, AuthorizationFragmentListener,
    UserInfoManagerListener, PermissionManagerListener {

    private const val appIdentifierFilename = "netIdAppIdentifiers.json"
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
                Log.w(javaClass.simpleName, "NetId Service configuration has been set already")
                return
            }

            this.netIdConfig = netIdConfig
            setupAuthManagerAndFetchConfiguration(netIdConfig.host)
            setupUserInfoManager()
            setupPermissionManager()
        }
    }

    fun transmitToken(token: String) {
        if (TokenUtil.isValidJwtToken(token)) {
            appAuthManager.setIdToken(token)
        } else {
            for (item in netIdServiceListeners) {
                item.onTransmittedInvalidToken()
            }
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
            return appAuthManager.getWebAuthorizationIntent(
                config.clientId,
                config.redirectUri,
                config.claims,
                authFlow,
                activity
            )?.let {
                when (authFlow) {
                    NetIdAuthFlow.Login, NetIdAuthFlow.LoginPermission ->
                        AuthorizationHardFragment(
                            this, availableAppIdentifiers, it, authFlow, config.loginLayerConfig.headlineText, config.loginLayerConfig.loginText, config.loginLayerConfig.continueText
                        )
                    NetIdAuthFlow.Permission ->
                        AuthorizationSoftFragment(
                            this, availableAppIdentifiers, it, config.permissionLayerConfig.logoId, config.permissionLayerConfig.headlineText, config.permissionLayerConfig.legalText, config.permissionLayerConfig.continueText
                        )
                }
            }
        }
        //TODO optimise error handling
        return null
    }

    fun continueButtonPermissionFlow(activity: Activity, authFlow: NetIdAuthFlow): MaterialButton {
        val button = MaterialButton(activity.applicationContext)

        button.id = R.id.fragmentAuthorizationButtonAgreeAndContinue
        button.letterSpacing = Resources.getSystem().getDimension(R.dimen.authorization_button_letter_spacing)
        button.text = Resources.getSystem().getText(R.string.authorization_soft_agree_and_continue_with_net_id)
        button.textSize = Resources.getSystem().getDimension(R.dimen.authorization_button_text_size)
        button.icon = Resources.getSystem().getDrawable(R.drawable.ic_netid_logo_small)
        button.iconSize = 20
        button.iconTint = null
        button.cornerRadius = Resources.getSystem().getDimension(R.dimen.authorization_button_corner_radius).toInt()
        button.strokeColor = Resources.getSystem().getColorStateList(R.color.authorization_close_button_color)
        button.strokeWidth =
            Resources.getSystem().getDimension(R.dimen.authorization_close_button_stroke_width).toInt()
        button.rippleColor =  Resources.getSystem().getColorStateList(R.color.authorization_close_button_color)

        return button
    }
/*
    <com.google.android.material.button.MaterialButton
    style="@style/Widget.MaterialComponents.Button.UnelevatedButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:textColor="@android:color/white"
    android:textAllCaps="true"
    android:letterSpacing="@dimen/authorization_button_letter_spacing"
     />*/

    fun continueButtonLoginFlow(activity: Activity, authFlow: NetIdAuthFlow): MaterialButton {
        val button = MaterialButton(activity.applicationContext)

        button.id = R.id.fragmentAuthorizationButtonAgreeAndContinue
        button.letterSpacing = Resources.getSystem().getDimension(R.dimen.authorization_button_letter_spacing)
        button.text = Resources.getSystem().getText(R.string.authorization_soft_agree_and_continue_with_net_id)
        button.textSize = Resources.getSystem().getDimension(R.dimen.authorization_button_text_size)
        button.cornerRadius = Resources.getSystem().getDimension(R.dimen.authorization_button_corner_radius).toInt()
        button.backgroundTintList = Resources.getSystem().getColorStateList(R.color.authorization_net_id_button_color)

        return button
    }

    fun permissionButtonForIdApp(activity: Activity, index: Int): MaterialButton? {
        val button = MaterialButton(activity.applicationContext)
        checkAvailableNetIdApplications(activity)
        // If there are no ID apps installed, return with an error.
        if (availableAppIdentifiers.isEmpty() || (index >= availableAppIdentifiers.count())) {
            this.onAuthorizationFailed(NetIdError(NetIdErrorProcess.Authentication, NetIdErrorCode.NoIdAppInstalled))
            return null
        }
        val result = availableAppIdentifiers[index]

        return button
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

    //TODO
//    fun authorize(packageName: String?, activity: Activity): Intent? {
//        if (handleConnection(activity.applicationContext, NetIdErrorProcess.Authentication)) {
//            packageName?.let { applicationId ->
//                openApp(activity.applicationContext, applicationId)
//            } ?: run {
//
//            }
//        }
//        return null
//    }

    fun fetchUserInfo(context: Context) {
        if (handleConnection(context, NetIdErrorProcess.UserInfo)) {
            var error: NetIdError? = null

            netIdConfig?.let { config ->
                appAuthManager.getAccessToken()?.let { token ->
                    userInfoManager.fetchUserInfo(config.host, token)
                } ?: {
                    error = NetIdError(NetIdErrorProcess.UserInfo, NetIdErrorCode.UnauthorizedClient)
                }
            } ?: run {
                error = NetIdError(NetIdErrorProcess.UserInfo, NetIdErrorCode.Uninitialized)
            }

            error?.let {
                for (item in netIdServiceListeners) {
                    item.onUserInfoFetchedWithError(it)
                }
            }
        }
    }

    fun fetchPermissions(context: Context, collapseSyncId: Boolean) {
        if (handleConnection(context, NetIdErrorProcess.PermissionRead)) {
            var error: NetIdError? = null
            appAuthManager.getPermissionToken()?.let { token ->
                permissionManager.fetchPermissions(token, collapseSyncId)
            } ?: run {
                error = NetIdError(NetIdErrorProcess.PermissionRead, NetIdErrorCode.UnauthorizedClient)
            }
            error?.let {
                for (item in netIdServiceListeners) {
                    item.onPermissionFetchFinishedWithError(it)
                }
            }
        }
    }


    fun updatePermission(context: Context, permission: NetIdPermissionUpdate, collapseSyncId: Boolean) {
        if (handleConnection(context, NetIdErrorProcess.PermissionWrite)) {
            var error: NetIdError? = null
            appAuthManager.getPermissionToken()?.let { token ->
                permissionManager.updatePermission(token, permission, collapseSyncId)
            } ?: run {
                error = NetIdError(NetIdErrorProcess.PermissionWrite, NetIdErrorCode.UnauthorizedClient)
            }
            error?.let {
                for (item in netIdServiceListeners) {
                    item.onPermissionUpdateFinishedWithError(it)
                }
            }

        }
    }

    private fun setupAuthManagerAndFetchConfiguration(host: String) {
        appAuthManager = AppAuthManagerFactory.createAppAuthManager()
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

// AppAuthManagerListener functions

    override fun onAuthorizationServiceConfigurationFetchedSuccessfully() {
        Log.i(javaClass.simpleName, "NetId Service Authorization Service Configuration fetched successfully")
        for (item in netIdServiceListeners) {
            item.onInitializationFinishedWithError(null)
        }
    }

    override fun onAuthorizationServiceConfigurationFetchFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "NetId Service Authorization Service Configuration fetch failed")
        for (item in netIdServiceListeners) {
            item.onInitializationFinishedWithError(error)
        }
    }

    override fun onAuthorizationSuccessful() {
        Log.i(javaClass.simpleName, "NetId Service Authorization successful")
        appAuthManager.getAccessToken()?.let {
            for (item in netIdServiceListeners) {
                item.onAuthenticationFinished(it)
            }
        }
    }

    override fun onAuthorizationFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "NetId Service Authorization failed")
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
        Log.i(javaClass.simpleName, "NetId Service close authentication")
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
        Log.i(javaClass.simpleName, "NetId Service will use app ${appIdentifier.name} for authentication")
    }

// UserInfoManagerListener functions

    override fun onUserInfoFetched(userInfo: UserInfo) {
        Log.i(javaClass.simpleName, "NetId Service user info fetched successfully")
        for (item in netIdServiceListeners) {
            item.onUserInfoFinished(userInfo)
        }
    }

    override fun onUserInfoFetchFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "NetId Service user info fetch failed")
        for (item in netIdServiceListeners) {
            item.onUserInfoFetchedWithError(error)
        }
    }

    // PermissionManagerListener functions
    override fun onPermissionsFetched(permissions: Permissions) {
        Log.i(javaClass.simpleName, "NetId Service permissions fetched successfully")
        for (item in netIdServiceListeners) {
            item.onPermissionFetchFinished(permissions)
        }
    }

    override fun onPermissionsFetchFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "NetId Service permissions fetch failed")
        for (item in netIdServiceListeners) {
            item.onPermissionFetchFinishedWithError(error)
        }
    }

    override fun onPermissionUpdated(subjectIdentifiers: SubjectIdentifiers) {
        Log.i(javaClass.simpleName, "NetId Service permissions updated successfully")
        for (item in netIdServiceListeners) {
            item.onPermissionUpdateFinished()
        }
    }

    override fun onPermissionUpdateFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "NetId Service permission update failed")
        for (item in netIdServiceListeners) {
            item.onPermissionUpdateFinishedWithError(error)
        }
    }
}
