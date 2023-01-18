package de.netid.mobile.sdk.example.buttonapp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import de.netid.mobile.sdk.api.*
import de.netid.mobile.sdk.example.buttonapp.databinding.ActivityMainBinding
import de.netid.mobile.sdk.model.PermissionReadResponse
import de.netid.mobile.sdk.model.PermissionResponseStatus
import de.netid.mobile.sdk.model.SubjectIdentifiers
import de.netid.mobile.sdk.model.UserInfo


class MainActivity : AppCompatActivity(), NetIdServiceListener, OnItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private var serviceState = ServiceState.Uninitialized

    /** Companion object for basic configuration of the [NetIdService] via [NetIdConfig] object.
     *  clientId and redirectUri are mandatory, all other parameters are optional.
     *  Nevertheless, we set a standard set of claims here.
     */
    companion object {
        private const val clientId = "ec54097f-83f6-4bb1-86f3-f7c584c649cd"
        private const val redirectUri = "https://eunid.github.io/redirectApp"
        private const val claims = "{\"userinfo\":{\"email\": {\"essential\": true}, \"email_verified\": {\"essential\": true}}}"
        private val permissionLayerConfig = null
        private val loginLayerConfig = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.activityMainStyleSpinner.onItemSelectedListener = this

        if (savedInstanceState != null) {
            return
        }

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

        binding.activityMainButtonEndSession.setOnClickListener {
            NetIdService.endSession()
        }

        val count = NetIdService.getCountOfAccountProviderApps(this)

        // This is the button to continue with the permission flow.
        // This button will always trigger app2web as there will be extra buttons for dedicated account provider apps.
        val permissionContinueButton = NetIdService.permissionContinueButtonFragment()
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.activityMainPermissionContainer, permissionContinueButton)
        }

        // If there are account provider apps installed, list their buttons here.
        // They will always trigger app2app.
        NetIdService.getKeysForAccountProviderApps().forEach {
            val appButton = NetIdService.accountProviderAppButtonFragment(it, NetIdAuthFlow.Permission, it)
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.activityMainPermissionContainer, appButton)
            }
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
        NetIdService.getKeysForAccountProviderApps().forEach {
            val appButton = NetIdService.accountProviderAppButtonFragment(it, NetIdAuthFlow.Login, it)
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.activityMainLoginContainer, appButton)
            }
        }

        updateElementsForServiceState()
    }

    private fun appendLog(message: String) {
        val currentText = binding.activityMainLogs.text.toString()
        val newText = currentText + message + "\n"
        binding.activityMainLogs.text = newText
    }

    private fun updateElementsForServiceState() {
        val isAuthorized =
            serviceState == ServiceState.AuthorizationSuccessful || serviceState == ServiceState.UserInfoFailed
                    || serviceState == ServiceState.UserInfoSuccessful || serviceState == ServiceState.PermissionWriteSuccessful || serviceState == ServiceState.PermissionWriteFailed
                    || serviceState == ServiceState.PermissionReadFailed || serviceState == ServiceState.PermissionReadSuccessful

        binding.activityMainButtonEndSession.isEnabled = isAuthorized
    }

    override fun onInitializationFinishedWithError(error: NetIdError?) {
        error?.let {
            appendLog("netID service initialization failed: ${it.code}, ${it.process}")
            serviceState = ServiceState.InitializationFailed

        } ?: run {
            appendLog("netID service initialized successfully")
            serviceState = ServiceState.InitializationSuccessful
        }
        updateElementsForServiceState()
    }

    override fun onAuthenticationFinished(accessToken: String) {
        appendLog("netID service authorized successfully\nAccess Token:\n$accessToken")
        serviceState = ServiceState.AuthorizationSuccessful
        updateElementsForServiceState()
    }

    override fun onAuthenticationFinishedWithError(error: NetIdError) {
        if (error.msg?.isNotEmpty() == true) {
            appendLog("netID service authorization failed: ${error.code}, ${error.process}, ${error.msg}")
        } else {
            appendLog("netID service authorization failed: ${error.code}, ${error.process}")
        }
        serviceState = ServiceState.AuthorizationFailed
        updateElementsForServiceState()
    }

    override fun onUserInfoFinished(userInfo: UserInfo) {
        appendLog("netID service user info - fetch finished successfully: $userInfo")
    }

    override fun onUserInfoFetchedWithError(error: NetIdError) {
        if (error.msg?.isNotEmpty() == true) {
            appendLog("netID service user info - fetch failed: ${error.code}, ${error.process}, ${error.msg}")
        } else {
            appendLog("netID service user info - fetch failed: ${error.code}, ${error.process}")
        }
    }

    override fun onSessionEnd() {
        appendLog("netID service session end")
        updateElementsForServiceState()
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

    // OnItemSelectedListener
    override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, id: Long) {
        NetIdService.setButtonStyle(NetIdButtonStyle.values()[position], this as Activity)
    }

    override fun onNothingSelected(arg0: AdapterView<*>?) {
        // TODO Auto-generated method stub
    }
}