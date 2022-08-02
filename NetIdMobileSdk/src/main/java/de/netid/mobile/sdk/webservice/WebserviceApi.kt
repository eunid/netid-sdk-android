package de.netid.mobile.sdk.webservice

import android.os.Looper
import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.api.NetIdErrorCode
import de.netid.mobile.sdk.api.NetIdErrorProcess
import de.netid.mobile.sdk.constants.WebserviceConstants
import de.netid.mobile.sdk.model.UserInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import android.os.Handler
import java.io.IOException

object WebserviceApi {

    private val client = OkHttpClient()

    fun performUserInfoRequest(
        accessToken: String,
        host: String,
        userInfoCallback: UserInfoCallback
    ) {
        val request = Request.Builder()
            .url(WebserviceConstants.HTTPS_PROTOCOL + host + WebserviceConstants.USER_INFO_PATH)
            .method(WebserviceConstants.GET_METHOD, null)
            .header(
                WebserviceConstants.AUTHORIZATION_HEADER,
                WebserviceConstants.AUTHORIZATION_BEARER_PREFIX + accessToken
            ).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                userInfoCallback.onUserInfoFetchFailed(
                    NetIdError(
                        NetIdErrorProcess.UserInfo,
                        NetIdErrorCode.Unknown
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val userInfo = Json.decodeFromString<UserInfo>(response.body?.string() ?: "")
                        Handler(Looper.getMainLooper()).post {
                            userInfoCallback.onUserInfoFetched(userInfo)
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            userInfoCallback.onUserInfoFetchFailed(
                                NetIdError(
                                    NetIdErrorProcess.UserInfo,
                                    NetIdErrorCode.InvalidRequest
                                )
                            )
                        }
                    }
                }
            }
        })
    }
}