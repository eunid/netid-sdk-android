package de.netid.mobile.sdk.ui

import android.app.Activity

interface AuthorizationFragmentListener {

    fun onAgreeAndContinueWithNetIdClicked(packageName: String?, activity: Activity)

    fun onCloseClicked()
}
