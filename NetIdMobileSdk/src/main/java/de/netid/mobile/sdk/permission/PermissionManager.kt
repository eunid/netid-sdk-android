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

package de.netid.mobile.sdk.permission

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.NetIdPermissionUpdate
import de.netid.mobile.sdk.model.PermissionReadResponse
import de.netid.mobile.sdk.model.PermissionResponseStatus
import de.netid.mobile.sdk.model.SubjectIdentifiers
import de.netid.mobile.sdk.webservice.PermissionReadCallback
import de.netid.mobile.sdk.webservice.PermissionUpdateCallback
import de.netid.mobile.sdk.webservice.WebserviceApi

internal class PermissionManager(private val listener: PermissionManagerListener) {

    fun fetchPermissions(accessToken: String, collapseSyncId: Boolean) {
        WebserviceApi.performPermissionReadRequest(
                accessToken,
                collapseSyncId,
                object : PermissionReadCallback {
                    override fun onPermissionsFetched(permissionResponse: PermissionReadResponse) {
                        listener.onPermissionsFetched(permissionResponse)
                    }

                    override fun onPermissionsFetchFailed(statusCode: PermissionResponseStatus, error: NetIdError) {
                        listener.onPermissionsFetchFailed(statusCode, error)
                    }
                })
    }


    fun updatePermission(accessToken: String, permission: NetIdPermissionUpdate, collapseSyncId: Boolean) {
        WebserviceApi.performPermissionUpdateRequest(
                accessToken,
                permission,
                collapseSyncId,
                object : PermissionUpdateCallback {
                    override fun onPermissionUpdated(subjectIdentifiers: SubjectIdentifiers) {
                        listener.onPermissionUpdated(subjectIdentifiers)
                    }

                    override fun onPermissionUpdateFailed(responseStatusCode: PermissionResponseStatus, error: NetIdError) {
                        listener.onPermissionUpdateFailed(responseStatusCode, error)
                    }
                })
    }
}
