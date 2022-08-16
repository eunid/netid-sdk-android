package de.netid.mobile.sdk.userinfo

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.UserInfo

interface UserInfoManagerListener {

    fun onUserInfoFetched(userInfo: UserInfo)

    fun onUserInfoFetchFailed(error: NetIdError)
}
