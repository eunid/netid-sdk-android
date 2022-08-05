package de.netid.mobile.sdk.webservice

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.UserInfo

/**
 * Defines functions which are called after a user info request is complete.
 */
interface UserInfoCallback {

    /**
     * Is called, when a user info request was successful. Provides the fetched information.
     *
     * @param userInfo a [UserInfo] instance representing the fetched information
     */
    fun onUserInfoFetched(userInfo: UserInfo)

    /**
     * Is called, when a user info request failed. Provides related error information.
     *
     * @param error a [NetIdError] instance describing the occurred error
     */
    fun onUserInfoFetchFailed(error: NetIdError)
}
