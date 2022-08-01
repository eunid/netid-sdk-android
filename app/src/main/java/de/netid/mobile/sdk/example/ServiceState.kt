package de.netid.mobile.sdk.example

enum class ServiceState {
    Uninitialized,
    InitializationFailed,
    InitializationSuccessful,
    AuthorizationFailed,
    AuthorizationSuccessful,
    UserInfoFailed,
    UserInfoSuccessful
}
