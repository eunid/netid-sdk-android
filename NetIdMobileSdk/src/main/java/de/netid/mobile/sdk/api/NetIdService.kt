package de.netid.mobile.sdk.api

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.appauth.AppAuthManager
import de.netid.mobile.sdk.appauth.AppAuthManagerFactory
import de.netid.mobile.sdk.appauth.AppAuthManagerListener
import de.netid.mobile.sdk.util.JsonUtil
import de.netid.mobile.sdk.util.PackageUtil

object NetIdService : AppAuthManagerListener {

    private const val appIdentifierFilename = "netIdAppIdentifiers"

    private var netIdConfig: NetIdConfig? = null
    private lateinit var appAuthManager: AppAuthManager
    private val availableAppIdentifiers = mutableListOf<String>()

    fun initialize(netIdConfig: NetIdConfig) {
        if (this.netIdConfig != null) {
            Log.w(javaClass.simpleName, "Configuration has been set already")
            return
        }

        this.netIdConfig = netIdConfig
        setupAuthManagerAndFetchConfiguration(netIdConfig.host)
    }

    private fun setupAuthManagerAndFetchConfiguration(host: String) {
        appAuthManager = AppAuthManagerFactory.createAppAuthManager()
        appAuthManager.listener = this
        appAuthManager.fetchAuthorizationServiceConfiguration(host)
    }

    fun getAuthorizationFragment(context: Context): Fragment {
        checkAvailableNetIdApplications(context)

        if (availableAppIdentifiers.size > 0) {
            // TODO Return fragment with adequate authorization buttons
            return Fragment()
        } else {
            // TODO Return fragment with web login button
            return Fragment()
        }
    }

    fun authorize(bundleIdentifier: String?) {
        bundleIdentifier?.let { identifier ->
            // TODO Perform App2App workflow
        } ?: run {
            // TODO Perform App2Web workflow
        }
    }

    private fun checkAvailableNetIdApplications(context: Context) {
        availableAppIdentifiers.clear()
        val appIdentifiers = JsonUtil.loadAppIdentifiers(appIdentifierFilename, context)
        availableAppIdentifiers.addAll(PackageUtil.getInstalledPackages(appIdentifiers, context.packageManager))
    }

    // AppAuthManagerListener functions

    override fun onAuthorizationServiceConfigurationFetchedSuccessfully() {
        Log.i(javaClass.simpleName, "Authorization Service Configuration fetched successfully")
    }

    override fun onAuthorizationServiceConfigurationFetchFailed(error: NetIdError) {
        Log.e(javaClass.simpleName, "Authorization Service Configuration fetch failed")
    }
}
