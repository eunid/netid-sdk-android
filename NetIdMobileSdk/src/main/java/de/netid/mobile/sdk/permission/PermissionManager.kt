package de.netid.mobile.sdk.permission

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.api.NetIdPermissionUpdate
import de.netid.mobile.sdk.model.Permissions
import de.netid.mobile.sdk.model.SubjectIdentifiers
import de.netid.mobile.sdk.webservice.PermissionReadCallback
import de.netid.mobile.sdk.webservice.PermissionUpdateCallback
import de.netid.mobile.sdk.webservice.WebserviceApi

class PermissionManager(private val listener: PermissionManagerListener) {

    fun fetchPermissions(accessToken: String, collapseSyncId: Boolean) {
        WebserviceApi.performPermissionReadRequest(
                accessToken,
                collapseSyncId,
                object : PermissionReadCallback {
                    override fun onPermissionsFetched(permissions: Permissions) {
                        listener.onPermissionsFetched(permissions)
                    }

                    override fun onPermissionsFetchFailed(error: NetIdError) {
                        listener.onPermissionsFetchFailed(error)
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

                    override fun onPermissionUpdateFailed(error: NetIdError) {
                        listener.onPermissionUpdateFailed(error)
                    }
                })
    }
}
