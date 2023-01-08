package de.netid.mobile.sdk.example.buttonapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import de.netid.mobile.sdk.api.*
import de.netid.mobile.sdk.example.buttonapp.R
import de.netid.mobile.sdk.example.buttonapp.databinding.ActivityMainBinding
import de.netid.mobile.sdk.model.PermissionReadResponse
import de.netid.mobile.sdk.model.PermissionResponseStatus
import de.netid.mobile.sdk.model.SubjectIdentifiers
import de.netid.mobile.sdk.model.UserInfo

class MainActivity : AppCompatActivity(), NetIdServiceListener {
    private lateinit var binding: ActivityMainBinding

    /** Companion object for basic configuration of the [NetIdService] via [NetIdConfig] object.
     *  clientId and redirectUri are mandatory, all other parameters are optional.
     *  Nevertheless, we set a standard set of claims here.
     */
    companion object {
        private const val clientId = "082531ba-1b22-4381-81b1-64add4b85b8a"
        private const val redirectUri = "https://netid-sdk-web.letsdev.de/redirect"
        private const val claims = "{\"userinfo\":{\"email\": {\"essential\": true}, \"email_verified\": {\"essential\": true}}}"
        private val permissionLayerConfig = null
        private val loginLayerConfig = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val netIdConfig = NetIdConfig(
            clientId = clientId,
            redirectUri = redirectUri,
            claims = claims,
            promptWeb = "consent",
            permissionLayerConfig = permissionLayerConfig,
            loginLayerConfig = loginLayerConfig
        )

        NetIdService.addListener(this)
        NetIdService.initialize(netIdConfig, this)

        // If there has been a saved session, we end it here - just for the sake of this demo to always start with a clean SDK.
        NetIdService.endSession()

        // This is the button to continue with the permission flow.
        // If account provider apps are installed, this button will use app2app, app2web otherwise.
        // If there are account provider apps installed, a list is shown beneath the button to choose one app.
        val permissionContinueButton = NetIdService.permissionContinueButtonFragment("")
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.activityMainPermissionContainer, permissionContinueButton)
        }

        // This is the button to continue with login/login+permission flow.
        // This button will always trigger app2web as there will be extra buttons for dedicated account provider apps.
        val loginContinueButton = NetIdService.loginContinueButtonFragment("", NetIdAuthFlow.Login)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.activityMainLoginContainer, loginContinueButton)
        }

        // If there are account provider apps installed, list their buttons here.
        // They will always trigger app2app.
        val count = NetIdService.getCountOfIdApps(this)
        for (i in 0 until count) {
            val appButton = NetIdService.appButtonFragment(i, NetIdAuthFlow.Login)
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.activityMainLoginContainer, appButton)
            }
        }
    }

    private fun appendLog(message: String) {
        val currentText = binding.activityMainLogs.text.toString()
        val newText = currentText + message + "\n"
        binding.activityMainLogs.text = newText
    }

    override fun onInitializationFinishedWithError(error: NetIdError?) {
        error?.let {
            appendLog("netID service initialization failed: ${it.code}, ${it.process}")
//            serviceState = ServiceState.InitializationFailed

        } ?: run {
            appendLog("netID service initialized successfully")
//            serviceState = ServiceState.InitializationSuccessful
        }
    }

    override fun onAuthenticationFinished(accessToken: String) {
        appendLog("netID service authorized successfully\nAccess Token:\n$accessToken")
//        serviceState = ServiceState.AuthorizationSuccessful
    }

    override fun onAuthenticationFinishedWithError(error: NetIdError) {
        if (error.msg?.isNotEmpty() == true) {
            appendLog("netID service authorization failed: ${error.code}, ${error.process}, ${error.msg}")
        } else {
            appendLog("netID service authorization failed: ${error.code}, ${error.process}")
        }
//        serviceState = ServiceState.AuthorizationFailed
    }

    override fun onUserInfoFinished(userInfo: UserInfo) {
        appendLog("netID service user info - fetch finished successfully: $userInfo")
//        serviceState = ServiceState.UserInfoSuccessful
    }

    override fun onUserInfoFetchedWithError(error: NetIdError) {
        if (error.msg?.isNotEmpty() == true) {
            appendLog("netID service user info - fetch failed: ${error.code}, ${error.process}, ${error.msg}")
        } else {
            appendLog("netID service user info - fetch failed: ${error.code}, ${error.process}")
        }
//        serviceState = ServiceState.UserInfoFailed
    }

    override fun onSessionEnd() {
        appendLog("netID service session end")
    }

    override fun onEncounteredNetworkError(error: NetIdError) {
        if (error.msg?.isNotEmpty() == true) {
            appendLog("netID service user info failed: ${error.code}, ${error.process}, ${error.msg}")
        } else {
            appendLog("netID service user info failed: ${error.code}, ${error.process}")
        }
    }

    override fun onAuthenticationCanceled(error: NetIdError) {
        appendLog("netID service user canceled authentication in process: ${error.process}")
        if (error.msg?.isNotEmpty() == true) {
            appendLog("original error message: ${error.msg}")
        }
    }

    override fun onPermissionUpdateFinished(subjectIdentifiers: SubjectIdentifiers) {
        TODO("Not yet implemented")
    }

    override fun onPermissionUpdateFinishedWithError(
        statusCode: PermissionResponseStatus,
        error: NetIdError
    ) {
        TODO("Not yet implemented")
    }

    override fun onPermissionFetchFinished(permissions: PermissionReadResponse) {
        TODO("Not yet implemented")
    }

    override fun onPermissionFetchFinishedWithError(
        statusCode: PermissionResponseStatus,
        error: NetIdError
    ) {
        TODO("Not yet implemented")
    }
}