# netID MobileSDK for Android

## About

## Initialize NetIDService

The `NetIdService` is the main interface to communicate with the netID SDK. It handles all the communication with the backend services and provides ui elements for the autherization flow.

First, construct a configuration object of type NetIdConfig for the NetIDService:
```kotlin
private lateinit var netIdConfig: NetIdConfig

companion object {
    private const val clientId = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
    private const val redirectUri = "https://netid-sdk-web.letsdev.de/redirect"
    private const val claims = "{\"userinfo\":{\"email\": {\"essential\": true}, \"email_verified\": {\"essential\": true}}}"
    private val permissionLayerConfig = null
    private val loginLayerConfig = null
}
...

netIdConfig = NetIdConfig(clientID, redirectUri, claims, permissionLayerConfig, loginLayerConfig)
```

The parameters have the following meaning:
| Parameter | Description |
| :---        |    :---   |
| clientId | The client id of your application. You can retrieve it from the netID Developer portal. This parameter is mandatory. |
| redirectUri | An URI that is used by your application to catch callbacks. You can retrieve it from the netID Developer portal. This parameter is mandatory. |
| claims | An array of strings, denoting additional claims that should be set during authorization. Can be null. |
| permissionLayerConfig | A set of strings, that can be used to customize the appearance of the layer for the permission flow. Can be null. |
| loginLayerConfig | A set of strings, that can be used to customize the appearance of the layer for the login flow. Can be null. |

As stated above, it is possible to customize certain aspects of the dialog presented for authorization. For example:
```kotlin
    private val loginLayerConfig = LoginLayerConfig("Headline text", "Login with app %s", "Continue text")
``` 

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
NetIdService.getAuthorizationFragment(this, authFlow, forceApp2App)
```
| Parameter | Description |
| :---        |    :---   |
| activity | The client id of your application. You can retrieve it from the netID Developer portal. This parameter is mandatory. |
| authFlow | An URI that is used by your application to catch callbacks. You can retrieve it from the netID Developer portal. This parameter is mandatory. |
| forceApp2App | A boolean value stating if you want to force app2app authorization. Can be omitted and defaults to `false`. |

You have to provide an instance of you app's activity so that the SDK can display a view for the authorization process itself.
With the parameter `authFlow`you decide, if you want to use `Permission`, `Login` or `Login + Permission` as authorization flow.
The optional parameter `forceApp2App` decides, if your app wants to use app2app only. If let alone, this parameter defaults to `false` meaning that if no ID provider apps are installed, the SDK will automatically fall back to app2web flow. If set to `true` and no ID provider apps are installed, this call will fail with an error.

Depending on the chosen flow, different views are presented to the user to deviide on how to proceed with the authorization process.


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
