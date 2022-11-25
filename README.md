# netID MobileSDK for Android

## About

Alongside the netID SDK, this repository has a sample app, demonstarting the implemented features.

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
| claims | An OIDC-compliant, URL-encoded JSON string, denoting additional claims that should be set during authorization. Can be null. |
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

Finally, initialize the NetIdService itself with the aforementioned configuration.
```kotlin
NetIdService.initialize(netIdConfig, this.applicationContext)
```

## Authorization

After the NetIDService has been initialized, subsequent calls to request authorization can be made. 
In the example app, you are presented with three choices as can be seen in this screenhsot.

<img src="images/netIdSdk_android_choose_authFlow.png" alt="netID SDK example app - chosse authFlow" style="width:200px;"/>

In your own app, you most likely will decide which flow to take without an user interaction. To initiate the authorization process, issue the following call to the NetIDService:
```kotlin
NetIdService.getAuthorizationFragment(this, authFlow, forceApp2App)
```
| Parameter | Description |
| :---        |    :---   |
| activity | The activity to attach this fragment to. This parameter is mandatory. |
| authFlow | Type of flow to use, can be either ``NetIdAuthFlow.Permission``, ``NetIdAuthFlow.Login`` or ``NetIdAuthFlow.LoginPermission``. This parameter is mandatory. |
| forceApp2App | If set to true, will yield an ``NetIdError`` if the are no ID apps installed. Otherwise, will use app2web flow automatically. Defaults to ``false``. |

You have to provide an instance of you app's activity so that the SDK can display a view for the authorization process itself.
With the parameter `authFlow`you decide, if you want to use `Permission`, `Login` or `Login + Permission` as authorization flow.
The optional parameter `forceApp2App` decides, if your app wants to use app2app only. If let alone, this parameter defaults to `false` meaning that if no ID provider apps are installed, the SDK will automatically fall back to app2web flow. If set to `true` and no ID provider apps are installed, this call will fail with an error.

Depending on the chosen flow, different views are presented to the user to decide on how to proceed with the authorization process.

<img src="images/netIdSdk_android_login_with_idApps.png" alt="netID SDK example app - chosse id app" style="width:150px;"><p><em>Login flow with installed id apps</em></p></img>
<img src="images/netIdSdk_android_permission_with_idApps.png" alt="netID SDK example app - chosse id app" style="width:150px;"><p><em>Permission flow with installed id apps</em></p></img>

If the user did decide on how to proceed with the login process (e.g. which ID provider to use), a redirect to actually execute the authorization is called automatically.

## Session persistence
The SDK implements session persistence. So if a user has been authorized successfully, this state stays persistent even when closing and reopening the app again.

To test this with the demo app, close the app once you are successfully authorized. Then, open the app again. After pressing the ```SDK initialisieren```-button, your session will be restored and you are again authorized. So there will be no need to press ```Authorisieren``` again.

To get rid of the current session, the ```NetIdService.endsession()``` has to be called explicitly. In the demo app, this is done by pressing ```Session beenden```. Note however, that this will destroy the current session only. There will be no logout on the server itself.

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

