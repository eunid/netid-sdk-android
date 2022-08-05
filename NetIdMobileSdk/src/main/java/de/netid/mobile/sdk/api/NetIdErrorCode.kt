package de.netid.mobile.sdk.api

enum class NetIdErrorCode {
    Timeout,
    NetworkError,
    Uninitialized,
    JsonDeserializationError,
    InvalidDiscoveryDocument,
    Unknown,
    AuthorizationCanceledByUser,
    MissingBrowser,
    InvalidRequest,
    UnauthorizedClient,
    AccessDenied,
    UnsupportedResponseType,
    InvalidScope,
    ServerError,
    TemporarilyUnavailable,
    ClientError,
    StateMismatch
}
