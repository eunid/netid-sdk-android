package de.netid.mobile.sdk.api

import android.content.Context
import android.util.Log
import de.netid.mobile.sdk.appauth.AppAuthManager
import de.netid.mobile.sdk.appauth.AppAuthManagerFactory
import de.netid.mobile.sdk.appauth.AppAuthManagerListener
import de.netid.mobile.sdk.util.JsonUtil
import de.netid.mobile.sdk.util.PackageUtil

object NetIdService : AppAuthManagerListener {

    private const val appIdentifierFilename = "netIdAppIdentifiers"

    private var netIdConfig: NetIdConfig? = null
    private lateinit var appAuthManager: AppAuthManager

    fun initialize(netIdConfig: NetIdConfig, context: Context) {
        if (this.netIdConfig != null) {
            Log.w(javaClass.simpleName, "Configuration has been set already")
            return
        }

        this.netIdConfig = netIdConfig

        checkAvailableNetIdApplications(context)
        setupAuthManagerAndFetchConfiguration(netIdConfig.host)
    }

    private fun checkAvailableNetIdApplications(context: Context) {
        val appIdentifiers = JsonUtil.loadAppIdentifiers(appIdentifierFilename, context)
        val availableAppIdentifiers = PackageUtil.getInstalledPackages(appIdentifiers, context.packageManager)
        // TODO Handle available app identifiers
    }

    private fun setupAuthManagerAndFetchConfiguration(host: String) {
        appAuthManager = AppAuthManagerFactory.createAppAuthManager()
        appAuthManager.listener = this
        appAuthManager.fetchAuthorizationServiceConfiguration(host)
    }

    // AppAuthManagerListener functions

    override fun onAuthorizationServiceConfigurationFetchedSuccessfully() {
        Log.i(javaClass.simpleName, "Authorization Service Configuration fetched successfully")
    }

    override fun onAuthorizationServiceConfigurationFetchFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "Authorization Service Configuration fetch failed")
    }
}
