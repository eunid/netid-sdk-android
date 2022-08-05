package de.netid.mobile.sdk.webservice

import android.os.Handler
import android.os.Looper
import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.api.NetIdErrorCode
import de.netid.mobile.sdk.api.NetIdErrorProcess
import de.netid.mobile.sdk.constants.WebserviceConstants
import de.netid.mobile.sdk.model.UserInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Provides functions to perform web requests.
 */
object WebserviceApi {

    /**
     * A [OkHttpClient] instance to enqueue web calls
     */
    private val client = OkHttpClient()

    /**
     * Performs a request to fetch information related to an authorized user.
     * The result of the request is provided via the given [UserInfoCallback] instance.
     *
     * @param accessToken a currently valid token to authenticate and identify a specific user
     * @param host the URL string of the host processing the request
     * @param userInfoCallback a [UserInfoCallback] instance receiving callbacks when the request is complete
     */
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
