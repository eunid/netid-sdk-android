package de.netid.mobile.sdk

enum class ServiceState {
    Uninitialized,
    InitializationFailed,
    InitializationSuccessful,
    AuthorizationFailed,
    AuthorizationSuccessful,
    UserInfoFailed,
    UserInfoSuccessful
}
