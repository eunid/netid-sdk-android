package de.netid.mobile.sdk.webservice

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.Permissions

interface PermissionReadCallback {

    fun onPermissionsFetched(permissions: Permissions)

    fun onPermissionsFetchFailed(error: NetIdError)
}
