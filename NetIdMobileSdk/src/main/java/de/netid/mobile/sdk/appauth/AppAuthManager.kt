package de.netid.mobile.sdk.appauth

import android.app.Activity
import android.content.Intent

interface AppAuthManager {

    var listener: AppAuthManagerListener?

    fun fetchAuthorizationServiceConfiguration(host: String)

    fun performWebAuthorization(clientId: String, redirectUri: String, activity: Activity)

    fun processAuthorizationIntent(requestCode: Int, data: Intent)
}
