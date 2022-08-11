package de.netid.mobile.sdk.model

enum class PermissionStatusCode(val code: String) {
    PermissionsFound("PERMISSIONS_FOUND"),
    PermissionsNotFound("PERMISSIONS_NOT_FOUND"),
    NoToken("NO_TOKEN"),
    TokenError("TOKEN_ERROR"),
    TappNotAllowed("TAPP_NOT_ALLOWED"),
    TpIdExistenceError("TPID_EXISTENCE_ERROR")
}
