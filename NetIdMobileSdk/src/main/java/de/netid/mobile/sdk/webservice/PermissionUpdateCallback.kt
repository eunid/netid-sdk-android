package de.netid.mobile.sdk.webservice

import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.model.SubjectIdentifiers

interface PermissionUpdateCallback {

    fun onPermissionUpdated(subjectIdentifiers: SubjectIdentifiers)

    fun onPermissionUpdateFailed(error: NetIdError)
}
