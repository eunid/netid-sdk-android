package de.netid.mobile.sdk.appauth

import android.app.Activity
import android.content.Intent

interface AppAuthManager {

    var listener: AppAuthManagerListener?

    fun fetchAuthorizationServiceConfiguration(host: String)

    fun getWebAuthorizationIntent(
        clientId: String,
        redirectUri: String,
        activity: Activity
    ): Intent?

    fun processAuthorizationIntent(data: Intent)

    fun getAccessToken(): String?

    fun getIdToken(): String?
}
