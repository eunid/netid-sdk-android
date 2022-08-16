package de.netid.mobile.sdk.example

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.netid.mobile.sdk.api.*
import de.netid.mobile.sdk.example.SdkContentBottomDialogFragment
import de.netid.mobile.sdk.example.databinding.ActivityMainBinding
import de.netid.mobile.sdk.model.Permissions
import de.netid.mobile.sdk.model.UserInfo

class MainActivity : AppCompatActivity(), NetIdServiceListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var netIdConfig: NetIdConfig

    private var serviceState = ServiceState.Uninitialized
    private lateinit var bottomDialogFragment: SdkContentBottomDialogFragment

    companion object {
        private const val clientId = "082531ba-1b22-4381-81b1-64add4b85b8a"
        private const val host = "broker.netid.de"
        private const val redirectUri =
            "de.netid.mobile.sdk.netidmobilesdk:/oauth2redirect/example-provider"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNetIdConfig()

        setupInitializeButton()
        setupAuthorizeButton()
        setupUserInfoButton()
        setupPermissionManagementButtons()
        setupEndSessionButton()
        updateElementsForServiceState()
    }

    private fun setupNetIdConfig() {
        netIdConfig = NetIdConfig(host, clientId, redirectUri)
        NetIdService.addListener(this)
    }

    private fun setupInitializeButton() {
        binding.activityMainButtonInitialize.setOnClickListener {
            it.isEnabled = false
            NetIdService.initialize(netIdConfig, this.applicationContext)
        }
    }

    private fun setupAuthorizeButton() {
        binding.activityMainButtonAuthorize.setOnClickListener {
            it.isEnabled = false
            bottomDialogFragment = SdkContentBottomDialogFragment()
            bottomDialogFragment.sdkContentFragment = NetIdService.getAuthorizationFragment(this)
            bottomDialogFragment.show(supportFragmentManager, null)
        }
    }

    private fun setupUserInfoButton() {
        binding.activityMainButtonUserInfo.setOnClickListener {
            it.isEnabled = false
            NetIdService.fetchUserInfo(this.applicationContext)
        }
    }

    private fun setupPermissionManagementButtons() {
        binding.activityMainButtonPermissionRead.setOnClickListener {
            it.isEnabled = false
            NetIdService.fetchPermissions(this.applicationContext, false)
        }


        binding.activityMainButtonPermissionWrite.setOnClickListener {
            it.isEnabled = false
            // these values are only for demonstration purpose
            val permission = NetIdPermissionUpdate(
                "VALID",
                "CPdfZIAPdfZIACnABCDECbCkAP_AAAAAAAYgIzJd9D7dbXFDefx_SPt0OYwW0NBXCuQCChSAA2AFVAOQcLQA02EaMATAhiACEQIAolIBAAEEHAFEAECQQIAEAAHsAgSEhAAKIAJEEBEQAAIQAAoKAAAAAAAIgAABoASAmBiQS5bmRUCAOIAQRgBIgggBCIADAgMBBEAIABgIAIIIgSgAAQAAAKIAAAAAARAAAASGgFABcAEMAPwAgoBaQEiAJ2AUiAxgBnwqASAEMAJgAXABHAEcALSAkEBeYDPh0EIABYAFQAMgAcgA-AEAALgAZAA0AB4AD6AIYAigBMACfAFwAXQAxABmADeAHMAPwAhgBLACYAE0AKMAUoAsQBbgDDAGiAPaAfgB-gEDAIoARaAjgCOgEpALEAWmAuYC6gF5AMUAbQA3ABxADnAHUAPQAi8BIICRAE7AKHAXmAwYBjADJAGVAMsAZmAz4BrADiwHjgPrAg0BDkhAbAAWABkAFwAQwAmABcADEAGYAN4AjgBSgCxAIoARwAlIBaQC5gGKANoAc4A6gB6AEggJEAScAz4B45KBAAAgABYAGQAOAAfAB4AEQAJgAXAAxABmADaAIYARwAowBSgC3AH4ARwAk4BaQC6gGKANwAdQBF4CRAF5gMsAZ8A1gCGoSBeAAgABYAFQAMgAcgA8AEAAMgAaAA8gCGAIoATAAngBvADmAH4AQgAhgBHACWAE0AKUAW4AwwB7QD8AP0AgYBFICNAI4ASkAuYBigDaAG4AOIAegBIgCdgFDgKRAXmAwYBkgDPoGsAayA4IB44EOREAYAQwA_AEiAJ2AUiAz4ZAHACGAEwARwBHAEnALzAZ8UgXAALAAqABkADkAHwAgABkADQAHkAQwBFACYAE8AKQAYgAzABzAD8AIYAUYApQBYgC3AGjAPwA_QCLQEcAR0AlIBcwC8gGKANoAbgA9ACLwEiAJOATsAocBeYDGAGSAMsAZ9A1gDWQHBAPHAhm.f_gAAAAAAsgA"
            )
            NetIdService.updatePermission(this.applicationContext, permission, true)
        }
    }

    private fun setupEndSessionButton() {
        binding.activityMainButtonEndSession.setOnClickListener {
            it.isEnabled = false
            appendLog("Net ID service session finished successfully")
            serviceState = ServiceState.InitializationSuccessful
            updateElementsForServiceState()
        }
    }

    private fun appendLog(message: String) {
        val currentText = binding.activityMainLogsTextView.text.toString()
        val newText = currentText + message + "\n"
        binding.activityMainLogsTextView.text = newText
    }

    private fun updateElementsForServiceState() {
        val isUninitialized =
            serviceState == ServiceState.Uninitialized || serviceState == ServiceState.InitializationFailed
        val isNotAuthorized =
            serviceState == ServiceState.InitializationSuccessful || serviceState == ServiceState.AuthorizationFailed
        val isAuthorized =
            serviceState == ServiceState.AuthorizationSuccessful || serviceState == ServiceState.UserInfoFailed
                    || serviceState == ServiceState.UserInfoSuccessful || serviceState == ServiceState.PermissionWriteSuccessful || serviceState == ServiceState.PermissionWriteFailed
                    || serviceState == ServiceState.PermissionReadFailed || serviceState == ServiceState.PermissionReadSuccessful

        binding.activityMainButtonInitialize.isEnabled = isUninitialized
        binding.activityMainButtonAuthorize.isEnabled = isNotAuthorized
        binding.activityMainButtonUserInfo.isEnabled = isAuthorized
        binding.activityMainButtonEndSession.isEnabled = isAuthorized
        binding.activityMainButtonPermissionWrite.isEnabled = isAuthorized
        binding.activityMainButtonPermissionRead.isEnabled = isAuthorized

        updateStateColorElements()
    }

    private fun updateStateColorElements() {
        val grayColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val greenColor = ContextCompat.getColor(this, android.R.color.holo_green_dark)
        val redColor = ContextCompat.getColor(this, android.R.color.holo_red_light)

        val grayStateList = ColorStateList.valueOf(grayColor)
        val greenStateList = ColorStateList.valueOf(greenColor)
        val redStateList = ColorStateList.valueOf(redColor)

        binding.activityMainStatusViewInitialize.backgroundTintList = when (serviceState) {
            ServiceState.Uninitialized -> grayStateList
            ServiceState.InitializationFailed -> redStateList
            else -> greenStateList
        }
        binding.activityMainStatusViewAuthorize.backgroundTintList = when (serviceState) {
            ServiceState.AuthorizationFailed -> redStateList
            ServiceState.Uninitialized, ServiceState.InitializationFailed, ServiceState.InitializationSuccessful -> grayStateList
            else -> greenStateList
        }
        binding.activityMainStatusViewUserInfo.backgroundTintList = when (serviceState) {
            ServiceState.UserInfoSuccessful -> greenStateList
            ServiceState.UserInfoFailed -> redStateList
            else -> grayStateList
        }
    }

    // NetIdServiceListener functions

    override fun onInitializationFinishedWithError(error: NetIdError?) {
        error?.let {
            appendLog("Net ID service initialization failed: ${it.code}, ${it.process}")
            serviceState = ServiceState.InitializationFailed
        } ?: run {
            appendLog("Net ID service initialized successfully")
            serviceState = ServiceState.InitializationSuccessful
        }
        updateElementsForServiceState()
    }

    override fun onAuthenticationFinished(accessToken: String) {
        appendLog("Net ID service authorized successfully\nAccess Token:\n$accessToken")
        serviceState = ServiceState.AuthorizationSuccessful
        bottomDialogFragment.dismiss()
        updateElementsForServiceState()
    }

    override fun onAuthenticationFinishedWithError(error: NetIdError) {
        appendLog("Net ID service authorization failed: ${error.code}, ${error.process}")
        serviceState = ServiceState.AuthorizationFailed
        updateElementsForServiceState()
        bottomDialogFragment.dismiss()
    }

    override fun onUserInfoFinished(userInfo: UserInfo) {
        appendLog("Net ID service user info -fetch finished successfully: $userInfo")
        serviceState = ServiceState.UserInfoSuccessful
        updateElementsForServiceState()
    }

    override fun onUserInfoFetchedWithError(error: NetIdError) {
        appendLog("Net ID service user info failed: ${error.code}, ${error.process}")
        serviceState = ServiceState.UserInfoFailed
        updateElementsForServiceState()
    }

    override fun onSessionEnd() {
        appendLog("Net ID service session end")
    }

    override fun onEncounteredNetworkError(error: NetIdError) {
        appendLog("Net ID service user info failed: ${error.code}, ${error.process}")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.network_error_alert_title)
        builder.setMessage(R.string.network_error_alert_description)
        builder.setPositiveButton(R.string.network_error_alert_action) { _, _ ->
            serviceState = when (error.process) {
                NetIdErrorProcess.Configuration -> ServiceState.InitializationFailed
                NetIdErrorProcess.Authentication -> ServiceState.AuthorizationFailed
                NetIdErrorProcess.UserInfo -> ServiceState.UserInfoFailed
                else -> ServiceState.Uninitialized
            }
            updateElementsForServiceState()
        }
        builder.show()
    }

    override fun onAuthenticationCanceled(error: NetIdError) {
        appendLog("Net ID service user canceled authentication in process: ${error.process}")
        serviceState = when (error.process) {
            NetIdErrorProcess.Configuration -> ServiceState.InitializationFailed
            NetIdErrorProcess.Authentication -> ServiceState.AuthorizationFailed
            NetIdErrorProcess.UserInfo -> ServiceState.UserInfoFailed
            else -> ServiceState.Uninitialized
        }
        updateElementsForServiceState()
        bottomDialogFragment.dismiss()
    }

    override fun onPermissionUpdateFinishedWithError(error: NetIdError) {
        appendLog("Net ID service permission -update failed with error: ${error.code}")
        serviceState = ServiceState.PermissionWriteFailed
        updateElementsForServiceState()
    }

    override fun onPermissionFetchFinishedWithError(error: NetIdError) {
        appendLog("Net ID service permission -fetch failed with error: ${error.code}")
        serviceState = ServiceState.PermissionReadFailed
        updateElementsForServiceState()
    }

    override fun onPermissionFetchFinished(permissions: Permissions) {
        appendLog("Net ID service permission -fetch finished successfully: $permissions")
        serviceState = ServiceState.PermissionReadSuccessful
        updateElementsForServiceState()

    }

    override fun onPermissionUpdateFinished() {
        appendLog("Net ID service permission -update finished successfully.")
        serviceState = ServiceState.PermissionWriteSuccessful
        updateElementsForServiceState()
    }
}
