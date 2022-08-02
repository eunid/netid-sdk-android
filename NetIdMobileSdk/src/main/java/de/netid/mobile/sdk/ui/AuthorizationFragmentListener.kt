package de.netid.mobile.sdk.ui

import android.content.Intent

interface AuthorizationFragmentListener {

    fun onAuthenticationFinished(response: Intent?)

    fun onAuthenticationFailed()

    fun onStartAuthentication()

    fun onCloseClicked()
}
