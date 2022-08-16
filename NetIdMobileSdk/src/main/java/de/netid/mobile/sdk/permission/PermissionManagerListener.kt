package de.netid.mobile.sdk.permission

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.Permissions
import de.netid.mobile.sdk.model.SubjectIdentifiers

interface PermissionManagerListener {

    fun onPermissionsFetched(permissions: Permissions)

    fun onPermissionsFetchFailed(error: NetIdError)

    fun onPermissionUpdated(subjectIdentifiers: SubjectIdentifiers)

    fun onPermissionUpdateFailed(error: NetIdError)
}
