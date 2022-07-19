package de.netid.mobile.sdk.appauth

import android.app.Activity

interface AppAuthManager {

    var listener: AppAuthManagerListener?

    fun fetchAuthorizationServiceConfiguration(host: String)

    fun performWebAuthorization(clientId: String, redirectUri: String, activity: Activity)
}
