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

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.UserInfo

/**
 * Defines functions which are called after a user info request is complete.
 */
internal interface UserInfoCallback {

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
