package de.netid.mobile.sdk.webservice

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.UserInfo

interface UserInfoCallback {
    fun onUserInfoFetched(userInfo: UserInfo)

    fun onUserInfoFetchFailed(error: NetIdError)
}