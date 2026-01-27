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
import de.netid.mobile.sdk.model.NetIdPermissionUpdate
import de.netid.mobile.sdk.model.UserInfo
import de.netid.mobile.sdk.model.permission.request.PermissionReadConfig
import de.netid.mobile.sdk.model.permission.request.PermissionWriteConfig
import de.netid.mobile.sdk.model.permission.response.PermissionResponseStatus
import de.netid.mobile.sdk.model.permission.response.PermissionUpdateErrorResponse
import de.netid.mobile.sdk.model.permission.response.PermissionUpdateResponse
import de.netid.mobile.sdk.model.permission.response.read.PermissionReadResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection

/**
 * Provides functions to perform web requests.
 */
internal object WebserviceApi {

    /**
     * A [OkHttpClient] instance to enqueue web calls
     */
    private val client = OkHttpClient()

    private val userInfoJsonObject = Json { ignoreUnknownKeys = true }

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
                        // Unknown JSON claims are ignored
                        val userInfo = userInfoJsonObject.decodeFromString<UserInfo>(response.body.string())
                        Handler(Looper.getMainLooper()).post {
                            userInfoCallback.onUserInfoFetched(userInfo)
                        }
                    } else if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
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
     * The result of the request is provided via the given [PermissionReadResponse] instance.
     *
     * @param accessToken a currently valid ID token to read permissions
     * @param configuration A configuration to define request properties
     * @param permissionReadCallback a [PermissionReadCallback] instance receiving callbacks when the request is complete
     */
    fun performPermissionReadRequest(
        accessToken: String,
        configuration: PermissionReadConfig,
        permissionReadCallback: PermissionReadCallback
    ) {
        val urlBuilder = HttpUrl.Builder()
            .scheme(WebserviceConstants.HTTPS_PROTOCOL)
            .host(WebserviceConstants.PERMISSION_READ_HOST)
            .addPathSegment(WebserviceConstants.PERMISSION_READ_PATH)

        configuration.queryParameter?.let { queryParameter ->
            urlBuilder.addQueryParameter(queryParameter.key, queryParameter.value)
        }

        val httpUrl = urlBuilder.build()

        val requestBuilder = Request.Builder()
            .url(httpUrl)
            .method(WebserviceConstants.GET_METHOD, null)
            .header(
                WebserviceConstants.AUTHORIZATION_HEADER,
                WebserviceConstants.AUTHORIZATION_BEARER_PREFIX + accessToken
            )
            .header(WebserviceConstants.ACCEPT_HEADER_KEY, configuration.acceptHeader)

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                permissionReadCallback.onPermissionsFetchFailed(
                    PermissionResponseStatus.UNKNOWN,
                    NetIdError(
                        NetIdErrorProcess.PermissionRead,
                        NetIdErrorCode.Unknown
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody: String = response.body.string()
                val permissionResponse = configuration.decodePermissionResponse(responseBody)

                response.use {
                    if (response.isSuccessful) {
                        Handler(Looper.getMainLooper()).post {
                            permissionReadCallback.onPermissionsFetched(permissionResponse)
                        }
                    } else {
                        // determine proper NetIDErrorCode
                        val errorCode: NetIdErrorCode =
                            if (permissionResponse.statusCode == PermissionResponseStatus.TPID_EXISTENCE_ERROR) {
                                NetIdErrorCode.Other
                            } else {
                                NetIdErrorCode.InvalidRequest
                            }

                        Handler(Looper.getMainLooper()).post {
                            permissionReadCallback.onPermissionsFetchFailed(
                                permissionResponse.statusCode,
                                NetIdError(
                                    NetIdErrorProcess.PermissionRead,
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

    /**
     * Performs a request to read permissions related to an authorized user.
     * The result of the request is provided via the given [PermissionReadResponse] instance.
     *
     * @param accessToken a currently valid ID token to read permissions
     * @param permissionUpdate a [NetIdPermissionUpdate] instance, defining the permission to update
     * @param configuration A configuration to define request properties
     * @param permissionUpdateCallback a [PermissionUpdateCallback] instance receiving callbacks when the request is complete
     */
    fun performPermissionUpdateRequest(
        accessToken: String,
        permissionUpdate: NetIdPermissionUpdate,
        configuration: PermissionWriteConfig,
        permissionUpdateCallback: PermissionUpdateCallback
    ) {
        val jsonElement = Json.encodeToJsonElement(permissionUpdate)
        val mediaType = WebserviceConstants.CONTENT_TYPE_PERMISSION_WRITE.toMediaType()
        val byteArray = jsonElement.toString().toByteArray()
        val body = byteArray.toRequestBody(mediaType)

        val urlBuilder = HttpUrl.Builder()
            .scheme(WebserviceConstants.HTTPS_PROTOCOL)
            .host(WebserviceConstants.PERMISSION_WRITE_HOST)
            .addPathSegment(WebserviceConstants.PERMISSION_WRITE_PATH)

        configuration.queryParameter?.let { queryParameter ->
            urlBuilder.addQueryParameter(queryParameter.key, queryParameter.value)
        }

        val httpUrl = urlBuilder.build()

        val requestBuilder = Request.Builder()
            .url(httpUrl)
            .method(WebserviceConstants.POST_METHOD, body)
            .header(
                WebserviceConstants.AUTHORIZATION_HEADER,
                WebserviceConstants.AUTHORIZATION_BEARER_PREFIX + accessToken
            )
            .header(WebserviceConstants.ACCEPT_HEADER_KEY, configuration.acceptHeader)
            .header(WebserviceConstants.CONTENT_TYPE_HEADER_KEY, WebserviceConstants.CONTENT_TYPE_PERMISSION_WRITE)

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                permissionUpdateCallback.onPermissionUpdateFailed(
                    PermissionResponseStatus.UNKNOWN,
                    NetIdError(
                        NetIdErrorProcess.PermissionWrite,
                        NetIdErrorCode.Unknown
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {
                var permissionUpdateErrorResponse: PermissionUpdateErrorResponse
                var permissionUpdateResponse: PermissionUpdateResponse
                // Unknown JSON claims are ignored, unknown ENUM values mapped to default
                val format = Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
                val responseBody = response.body.string()

                response.use {
                    if (response.isSuccessful) {
                        // in case of success a SubjectIdentifier object is returned
                        permissionUpdateResponse = format.decodeFromString(responseBody)
                        Handler(Looper.getMainLooper()).post {
                            permissionUpdateCallback.onPermissionUpdated(permissionUpdateResponse.subjectIdentifiers)
                        }
                    } else {
                        permissionUpdateErrorResponse = format.decodeFromString(responseBody)

                        // determine proper NetIDErrorCode
                        val errorCode: NetIdErrorCode =
                            if (permissionUpdateErrorResponse.statusCode == PermissionResponseStatus.TPID_EXISTENCE_ERROR) {
                                NetIdErrorCode.Other
                            } else {
                                NetIdErrorCode.InvalidRequest
                            }

                        Handler(Looper.getMainLooper()).post {
                            permissionUpdateCallback.onPermissionUpdateFailed(
                                permissionUpdateErrorResponse.statusCode,
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
