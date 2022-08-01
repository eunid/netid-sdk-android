package de.netid.mobile.sdk.api

import de.netid.mobile.sdk.model.UserInfo

interface NetIdServiceListener {
    fun onInitializationFinishedWithError(error: NetIdError?)

    fun onAuthenticationFinished(accessToken: String)

    fun onAuthenticationFinishedWithError(error: NetIdError)

    fun onUserInfoFinished(userInfo: UserInfo)

    fun onUserInfoFetchedWithError(error: NetIdError)

    fun onSessionEnd()

    fun onEncounteredNetworkError(error: NetIdError)

    fun onAuthenticationCanceled(error: NetIdError)
}
