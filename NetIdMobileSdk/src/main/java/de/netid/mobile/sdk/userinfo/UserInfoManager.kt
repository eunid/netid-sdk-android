package de.netid.mobile.sdk.userinfo

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.UserInfo
import de.netid.mobile.sdk.webservice.UserInfoCallback
import de.netid.mobile.sdk.webservice.WebserviceApi

class UserInfoManager(private val listener: UserInfoManagerListener) {

    fun fetchUserInfo(host: String, accessToken: String) {
        WebserviceApi.performUserInfoRequest(
            accessToken,
            host,
            object : UserInfoCallback {
                override fun onUserInfoFetched(userInfo: UserInfo) {
                    listener.onUserInfoFetched(userInfo)
                }

                override fun onUserInfoFetchFailed(error: NetIdError) {
                    listener.onUserInfoFetchFailed(error)
                }
            })
    }
}
