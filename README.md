# netID MobileSDK for Android

## About

## Initialize NetIDService

First, construct a configuration object of type NetIdConfig for the NetIDService:
```kotlin
private lateinit var netIdConfig: NetIdConfig

companion object {
    private const val host = "broker.netid.de"
    private const val clientId = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
    private const val redirectUri = "https://netid-sdk-web.letsdev.de/redirect"
}
...

netIdConfig = NetIdConfig(host, clientID, redirectUri, "", emptyMap(), nil, nil)
```

The parameters have the following meaning:
| Parameter | Description |
| :---        |    :---   |
| host | The name of the broker for the SSO service. This Parameter is optional. If omitted, this is set to the default broker broker.netid.de |
| clientId | The client id of your application. You can retrieve it from the netID Developer portal. This parameter is mandatory. |
| redirectUri | An URI that is used by your application to catch callbacks. You can retrieve it from the netID Developer portal. This parameter is mandatory. |
| originUrlScheme | Used for creating deep links, not in use anymore (will be removed) |
| claims | An array of strings, denoting additional claims that should be set during authorization. Can be nil. |
| loginLayerConfig | A set of strings, that can be used to customize the appearance of the layer for the login flow. Can be nil. |
| permissionLayerConfig | A set of strings, that can be used to customize the appearance of the layer for the permission flow. Can be nil. |


Then, register your application as a listener to receive all callbacks made by the `NetIdService`.
```kotlin
NetIdService.addListener(this)
```


And then, initialize the NetIdService itself with the aforementioned configuration.
```kotlin
NetIdService.initialize(netIdConfig, this.applicationContext)
```

## Authorization

After the NetIDService has been initialized, subsequent calls to request authorization can be made. To initiate the authorization process, issue the following call to the NetIDService:
```kotlin
NetIdService.getAuthorizationFragment(this, authFlow)
```
You have to provide an instance of you app's view controller so that the SDK can display a view for the authorization process itself.
The optional parameter authFlow decides, which authorization flow to use.
If the user did decide on how to proceed with the login process (e.g. which ID provider to use), a redirect to actually execute the authorization is called automatically.

## Using the authorized service

Subsequent calls now can be made to use different aspects of the service.


```kotlin
NetIdService.endSession()
```
Use this call to end a session. On the listener `onEndSession` is called signalling success of the operation. All objects regarding authorization (e.g. tokens) will get discarded. However, the service itself will still be available. A new call to `getAuthorizationFragment` will trigger a new authorization process.

```kotlin
NetIdService.fetchUserInfo(this.applicationContext)
```
Fetches the user information object. On success `onFetchUserInfo` is called on the delegate, returning the requested information. Otherwise `onFetchUserInfoWithError` gets called, returning a description of the error.

```kotlin
NetIdService.fetchPermissions(this.applicationContext)
```
Fetches the permissions object. On success `onFetchPermissions` is called on the delegate, returning the requested information. Otherwise `onFetchPermissionsWithError` gets called, returning a description of the error.

```kotlin
NetIdService.updatePermissions(this.applicationContext)
```
Updates the permissions object. On success `onUpdatePermissions` is called on the delegate, returning the requested information. Otherwise `onUpdatePermissionsWithError` gets called, returning a description of the error.

```kotlin   
NetIdService.transmitToken(this.applicationContext, token)
```
Sets the id token to be used by the SDK. When using app2web flow, it is not necessary to set the token because the SDK itself gets a callback and can extract the id token. But in the app2app flow, the application is getting the authorization information directly. And thus, the application has to set the token for further use in the SDK.

## SDK configuration for ID provider apps

It is possible to configure the SDK to make use of the apps of different ID providers. Right now, two of them are supported.
The configuration resides in the file `netIdAppIdentifiers.json` inside the SDK. As this is an internal part of the SDK, it is not meant to be set via an interface nor API.

```json
{
  "netIdAppIdentifiers": [
    {
      "id": 1,
      "name": "GMX",
      "icon": "logo_gmx",
      "typeFaceIcon": "typeface_gmx",
      "backgroundColor": "#FF1E50A0",
      "foregroundColor": "#FFFFFFFF",
      "iOS": {
        "bundleIdentifier": "de.gmx.mobile.ios.mail",
        "scheme": "gmxmail",
        "universalLink": "https://sso.gmx.net/authorize-app2app"
      },
      "android": {
        "applicationId": "de.gmx.mobile.android.mail",
        "verifiedAppLink": "https://sso.gmx.net/authorize-app2app"
      }
    },
    {
      "id": 2,
      "name": "WEB.DE",
      "icon": "logo_web_de",
      "typeFaceIcon": "typeface_webde",
      "backgroundColor": "#FFFFD800",
      "foregroundColor": "#FF333333",
      "iOS": {
        "bundleIdentifier": "de.web.mobile.ios.mail",
        "scheme": "webdemail",
        "universalLink": "https://sso.web.de/authorize-app2app"
      },
      "android": {
        "applicationId": "de.web.mobile.android.mail",
        "verifiedAppLink": "https://sso.web.de/authorize-app2app"
      }
    }
  ]
}
````
