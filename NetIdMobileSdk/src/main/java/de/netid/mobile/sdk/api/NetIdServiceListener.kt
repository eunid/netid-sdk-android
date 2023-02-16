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

package de.netid.mobile.sdk.api

import de.netid.mobile.sdk.model.PermissionReadResponse
import de.netid.mobile.sdk.model.PermissionResponseStatus
import de.netid.mobile.sdk.model.SubjectIdentifiers
import de.netid.mobile.sdk.model.UserInfo

interface NetIdServiceListener {
    /**
     * Callback function that gets called when the SDK could not be initialized correctly.
     * In this case, a ``NetIdError`` is returned which holds more information about the error.
     * @param error Error description.
     */
    fun onInitializationFinishedWithError(error: NetIdError?)

    /**
     * Callback function that gets called when the authentication process finished successfully.
     * In this case, an access token is returned.
     * @param token Access token.
     */
    fun onAuthenticationFinished(accessToken: String)

    /**
     * Callback function when user information could not be retrieved.
     * In this case, a ``NetIdError`` is returned which holds more information about the error.
     * @param error Error description.
     */
    fun onAuthenticationFinishedWithError(error: NetIdError)

    /**
     * Callback function that gets called when user information could be retrieved successfully.
     * @param userInfo Filled out user information.
     */
    fun onUserInfoFinished(userInfo: UserInfo)

    /**
     * Callback function that gets called when user information could not be retrieved.
     * In this case, a ``NetIdError`` is returned which holds more information about the error.
     * @param error Error description.
     */
    fun onUserInfoFetchedWithError(error: NetIdError)

    /**
     * Callback function that gets called when a session ends.
     */
    fun onSessionEnd()

    /**
     * Delegate function that gets called when a network error occured.
     * In this case, a ``NetIdError`` is returned which holds more information about the error.
     * @param error Error description.
     */
    fun onEncounteredNetworkError(error: NetIdError)

    /**
     * Callback function that gets called when the authentication process got canceled.
     * In this case, a ``NetIdError`` is returned which holds more information about the error.
     * @param error Error description.
     */
    fun onAuthenticationCanceled(error: NetIdError)

    /**
     * Callback function that gets called when permissions were fetched successfully.
     * @param subjectIdentifiers Fetched permissions.
     */
    fun onPermissionUpdateFinished(subjectIdentifiers: SubjectIdentifiers)

    /**
     * Callback function that gets called when user information could not be retrieved.
     * In this case, a ``NetIdError`` and ``PermissionResponseStatus``are returned which hold more information about the error.
     * @param statusCode Status of the last permission command.
     * @param error Error description.
     */
    fun onPermissionUpdateFinishedWithError(statusCode: PermissionResponseStatus, error: NetIdError)

    /**
     * Callback function that gets called when permissions were updated successfully.
     * @param permissions Updated subject identifiers.
     */
    fun onPermissionFetchFinished(permissions: PermissionReadResponse)

    /**
     * Callback function that gets called when permissions could not be updated.
     * In this case, a ``NetIdError`` and ``PermissionResponseStatus``are returned which hold more information about the error.
     * @param statusCode Status of the last permission command.
     * @param error Error description.
     */
    fun onPermissionFetchFinishedWithError(statusCode: PermissionResponseStatus, error: NetIdError)
}
