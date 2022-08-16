package de.netid.mobile.sdk.constants

object WebserviceConstants {
    const val AUTHORIZATION_BEARER_PREFIX = "Bearer "
    const val AUTHORIZATION_HEADER = "Authorization"
    const val ACCEPT_HEADER_KEY = "Accept"
    const val CONTENT_TYPE_HEADER_KEY = "Content-Type"
    const val ACCEPT_HEADER_PERMISSION_READ = "application/vnd.netid.permission-center.netid-user-status-v1+json"
    const val ACCEPT_HEADER_PERMISSION_READ_AUDIT = "application/vnd.netid.permission-center.netid-user-status-audit-v1+json"
    const val ACCEPT_HEADER_PERMISSION_WRITE = "application/vnd.netid.permission-center.netid-subject-status-v1+json"
    const val ACCEPT_HEADER_PERMISSION_WRITE_AUDIT = "application/vnd.netid.permission-center.netid-subject-status-audit-v1+json"
    const val CONTENT_TYPE_PERMISSION_WRITE = "application/vnd.netid.permission-center.netid-permissions-v2+json"
    const val GET_METHOD = "GET"
    const val POST_METHOD = "POST"
    const val HTTPS_PROTOCOL = "https://"
    const val USER_INFO_PATH = "/userinfo"
    const val PERMISSION_READ_HOST = "einwilligungsspeicher.netid.de"
    const val PERMISSION_READ_PATH = "/netid-user-status"
    const val PERMISSION_WRITE_HOST = "einwilligen.netid.de"
    const val PERMISSION_WRITE_PATH = "/netid-permissions"
}
