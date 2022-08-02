package de.netid.mobile.sdk.ui

import android.content.Intent
import de.netid.mobile.sdk.model.AppIdentifier

interface AuthorizationFragmentListener {

    fun onAuthenticationFinished(response: Intent?)

    fun onAuthenticationFailed()

    fun onCloseClicked()

    fun onAppButtonClicked(appIdentifier: AppIdentifier)
}
