package de.netid.mobile.sdk.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.appauth.AppAuthManager
import de.netid.mobile.sdk.appauth.AppAuthManagerFactory
import de.netid.mobile.sdk.appauth.AppAuthManagerListener
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.model.UserInfo
import de.netid.mobile.sdk.ui.AuthorizationFragment
import de.netid.mobile.sdk.ui.AuthorizationFragmentListener
import de.netid.mobile.sdk.util.JsonUtil
import de.netid.mobile.sdk.util.ReachabilityUtil
import de.netid.mobile.sdk.webservice.UserInfoCallback
import de.netid.mobile.sdk.webservice.WebserviceApi

object NetIdService : AppAuthManagerListener, AuthorizationFragmentListener {

    private const val appIdentifierFilename = "netIdAppIdentifiers.json"
    private const val netIdAuthorizeKey = "netid_authorize"

    private var netIdConfig: NetIdConfig? = null
    private lateinit var appAuthManager: AppAuthManager
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
        }
    }

    fun getAuthorizationFragment(activity: Activity): Fragment? {
        checkAvailableNetIdApplications(activity)
        netIdConfig?.let { config ->
            return appAuthManager.getWebAuthorizationIntent(
                config.clientId,
                config.redirectUri,
                activity
            )?.let {
                AuthorizationFragment(
                    this, availableAppIdentifiers, it
                )
            }
        }
        //TODO optimise error handling
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
            netIdConfig?.let { config ->
                appAuthManager.getAccessToken()?.let { token ->
                    WebserviceApi.performUserInfoRequest(
                        token,
                        config.host,
                        object : UserInfoCallback {
                            override fun onUserInfoFetched(userInfo: UserInfo) {
                                for (item in netIdServiceListeners) {
                                    item.onUserInfoFinished(userInfo)
                                }
                            }

                            override fun onUserInfoFetchFailed(error: NetIdError) {
                                for (item in netIdServiceListeners) {
                                    item.onUserInfoFetchedWithError(error)
                                }
                            }
                        })
                }
            }
        }
    }

    private fun setupAuthManagerAndFetchConfiguration(host: String) {
        appAuthManager = AppAuthManagerFactory.createAppAuthManager()
        appAuthManager.listener = this
        appAuthManager.fetchAuthorizationServiceConfiguration(host)
    }

    private fun checkAvailableNetIdApplications(context: Context) {
        availableAppIdentifiers.clear()
        val appIdentifiers = JsonUtil.loadAppIdentifiers(appIdentifierFilename, context)
        availableAppIdentifiers.addAll(appIdentifiers)
    }

    private fun openApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.putExtra(netIdAuthorizeKey, context.applicationInfo.packageName)
        context.startActivity(intent)
    }

    // AppAuthManagerListener functions

    override fun onAuthorizationServiceConfigurationFetchedSuccessfully() {
        Log.i(
            javaClass.simpleName,
            "NetId Service Authorization Service Configuration fetched successfully"
        )
        for (item in netIdServiceListeners) {
            item.onInitializationFinishedWithError(null)
        }
    }

    override fun onAuthorizationServiceConfigurationFetchFailed(error: NetIdError) {
        Log.e(
            javaClass.simpleName,
            "NetId Service Authorization Service Configuration fetch failed"
        )
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
        TODO("Not yet implemented")
    }

    override fun onStartAuthentication() {
        TODO("Not yet implemented")
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
}
