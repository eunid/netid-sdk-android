// Copyright 2022 European netID Foundation (https://enid.foundation)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.netid.mobile.sdk.webservice

import android.net.Uri
import android.os.Handler
import android.os.Looper
import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.api.NetIdErrorCode
import de.netid.mobile.sdk.api.NetIdErrorProcess
import de.netid.mobile.sdk.constants.WebserviceConstants
import de.netid.mobile.sdk.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
     * @param userinfoEndpoint the URI of the userinfo endpoint to call
     * @param userInfoCallback a [UserInfoCallback] instance receiving callbacks when the request is complete
     */
    fun performUserInfoRequest(
        accessToken: String,
        userinfoEndpoint: Uri,
        userInfoCallback: UserInfoCallback
    ) {
        val request = Request.Builder()
            .url(userinfoEndpoint.toString())
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
                    } else if (response.code == 401) {
                        Handler(Looper.getMainLooper()).post {
                            userInfoCallback.onUserInfoFetchFailed(
                                NetIdError(
                                    NetIdErrorProcess.UserInfo,
                                    NetIdErrorCode.UnauthorizedClient
                                )
                            )
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

    /**
     * Performs a request to read permissions related to an authorized user.
     * The result of the request is provided via the given [PermissionReponse] instance.
     *
     * @param accessToken a currently valid ID token to read permissions
     * @param collapseSyncId If `true`, the response will not contain the sync id
     * @param permissionReadCallback a [PermissionReadCallback] instance receiving callbacks when the request is complete
     */
    fun performPermissionReadRequest(
        accessToken: String,
        collapseSyncId: Boolean,
        permissionReadCallback: PermissionReadCallback
    ) {
        val requestBuilder = Request.Builder()
            .url(WebserviceConstants.HTTPS_PROTOCOL + WebserviceConstants.PERMISSION_READ_HOST + WebserviceConstants.PERMISSION_READ_PATH)
            .method(WebserviceConstants.GET_METHOD, null)
            .header(
                WebserviceConstants.AUTHORIZATION_HEADER,
                WebserviceConstants.AUTHORIZATION_BEARER_PREFIX + accessToken
            )

        if (collapseSyncId) {
            requestBuilder.header(
                WebserviceConstants.ACCEPT_HEADER_KEY,
                WebserviceConstants.ACCEPT_HEADER_PERMISSION_READ
            )
        } else {
            requestBuilder.header(
                WebserviceConstants.ACCEPT_HEADER_KEY,
                WebserviceConstants.ACCEPT_HEADER_PERMISSION_READ_AUDIT
            )
        }
        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                permissionReadCallback.onPermissionsFetchFailed(
                    PermissionStatusCode.UNKNOWN,
                    NetIdError(
                        NetIdErrorProcess.PermissionRead,
                        NetIdErrorCode.Unknown
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {
                var permissionResponse: PermissionReponse
                val format = Json { ignoreUnknownKeys = true }
                val body: String = response.body?.string() ?: ""

                response.use {
                    if (response.isSuccessful) {
                        permissionResponse = format.decodeFromString(body)
                        Handler(Looper.getMainLooper()).post {
                            permissionReadCallback.onPermissionsFetched(permissionResponse)
                        }
                    } else {
                        // parse response for status_code for error details
                        permissionResponse = format.decodeFromString(body)

                        val responseStatus: PermissionStatusCode =
                            // Check if status_code is known and return in case it is
                            if (PermissionStatusCode.values().any {
                                    it.name == permissionResponse.statusCode }) {
                                PermissionStatusCode.valueOf(permissionResponse.statusCode)
                            } else {
                                //in case of unexpected status_code value -> set UNKNOWN
                                PermissionStatusCode.UNKNOWN
                            }

                        // determine proper NetIDErrorCode
                        val errorCode: NetIdErrorCode =
                            if (responseStatus == PermissionStatusCode.TPID_EXISTENCE_ERROR){
                                NetIdErrorCode.Other
                            } else {
                                NetIdErrorCode.InvalidRequest
                            }

                        Handler(Looper.getMainLooper()).post {
                            permissionReadCallback.onPermissionsFetchFailed(
                                responseStatus,
                                NetIdError(
                                    NetIdErrorProcess.PermissionRead,
                                    errorCode
                                )
                            )
                        }
                    }
                }
            }
        })
    }

    /**
     * Performs a request to read permissions related to an authorized user.
     * The result of the request is provided via the given [PermissionReponse] instance.
     *
     * @param accessToken a currently valid ID token to read permissions
     * @param permissionUpdate a [NetIdPermissionUpdate] instance, defining the permission to update
     * @param collapseSyncId If `true`, the response will not contain the sync id
     * @param permissionUpdateCallback a [PermissionUpdateCallback] instance receiving callbacks when the request is complete
     */
    fun performPermissionUpdateRequest(
        accessToken: String,
        permissionUpdate: NetIdPermissionUpdate,
        collapseSyncId: Boolean,
        permissionUpdateCallback: PermissionUpdateCallback
    ) {
        val jsonElement = Json.encodeToJsonElement(permissionUpdate)
        val mediaType = "application/vnd.netid.permission-center.netid-permissions-v2+json".toMediaType()
        val byteArray = jsonElement.toString().toByteArray()
        val body = byteArray.toRequestBody(mediaType)


        val requestBuilder = Request.Builder()
            .url(WebserviceConstants.HTTPS_PROTOCOL + WebserviceConstants.PERMISSION_WRITE_HOST + WebserviceConstants.PERMISSION_WRITE_PATH)
            .method(WebserviceConstants.POST_METHOD, body)
            .header(
                WebserviceConstants.AUTHORIZATION_HEADER,
                WebserviceConstants.AUTHORIZATION_BEARER_PREFIX + accessToken
            )

        if (collapseSyncId) {
            requestBuilder.header(
                WebserviceConstants.ACCEPT_HEADER_KEY,
             WebserviceConstants.ACCEPT_HEADER_PERMISSION_WRITE
            )
        } else {
            requestBuilder.header(
                WebserviceConstants.ACCEPT_HEADER_KEY,
                WebserviceConstants.ACCEPT_HEADER_PERMISSION_WRITE_AUDIT
            )
        }

        val request = requestBuilder.header(
            WebserviceConstants.CONTENT_TYPE_HEADER_KEY,
            WebserviceConstants.CONTENT_TYPE_PERMISSION_WRITE
        )
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                permissionUpdateCallback.onPermissionUpdateFailed(
                    PermissionStatusCode.UNKNOWN,
                    NetIdError(
                        NetIdErrorProcess.PermissionWrite,
                        NetIdErrorCode.Unknown
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {
                var permissionResponse: PermissionUpdateResponse
                val format = Json { ignoreUnknownKeys = true }
                val responseBody = response.body?.string() ?: ""

                response.use {

                    if (response.isSuccessful) {
                        permissionResponse = format.decodeFromString(responseBody)
                        Handler(Looper.getMainLooper()).post {
                            // In case of a successful call subjectIdentifier is never null
                            permissionUpdateCallback.onPermissionUpdated(permissionResponse.subjectIdentifiers!!)
                        }
                    } else {
                        permissionResponse = format.decodeFromString(responseBody)

                        val responseStatus: PermissionStatusCode =
                            // Check if status_code is known and return in case it is
                            if (PermissionStatusCode.values().any {
                                    it.name == permissionResponse.statusCode }) {
                                PermissionStatusCode.valueOf(permissionResponse.statusCode!!)
                            } else {
                                //in case of unexpected status_code value/null -> set UNKNOWN
                                PermissionStatusCode.UNKNOWN
                            }

                        // determine proper NetIDErrorCode
                        val errorCode: NetIdErrorCode =
                            if (responseStatus == PermissionStatusCode.TPID_EXISTENCE_ERROR){
                                NetIdErrorCode.Other
                            } else {
                                NetIdErrorCode.InvalidRequest
                            }

                        Handler(Looper.getMainLooper()).post {
                            permissionUpdateCallback.onPermissionUpdateFailed(
                                responseStatus,
                                NetIdError(
                                    NetIdErrorProcess.PermissionWrite,
                                    errorCode,
                                    msg = responseBody
                                )
                            )
                        }
                    }
                }
            }
        })
    }
}
