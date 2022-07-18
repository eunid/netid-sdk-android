package de.netid.mobile.sdk.api

import android.util.Log
import de.netid.mobile.sdk.appauth.AppAuthManager
import de.netid.mobile.sdk.appauth.AppAuthManagerFactory
import de.netid.mobile.sdk.appauth.AppAuthManagerListener

object NetIdService : AppAuthManagerListener {

    private var netIdConfig: NetIdConfig? = null
    private lateinit var appAuthManager: AppAuthManager

    fun initialize(netIdConfig: NetIdConfig) {
        if (this.netIdConfig != null) {
            Log.w(javaClass.simpleName, "Configuration has been set already")
            return
        }

        this.netIdConfig = netIdConfig
        appAuthManager = AppAuthManagerFactory.createAppAuthManager()
        appAuthManager.listener = this
    }

    // AppAuthManagerListener functions

    override fun onAuthorizationServiceConfigurationFetchedSuccessfully() {
        Log.i(javaClass.simpleName, "Authorization Service Configuration fetched successfully")
    }

    override fun onAuthorizationServiceConfigurationFetchFailed() {
        Log.e(javaClass.simpleName, "Authorization Service Configuration fetch failed")
    }
}
