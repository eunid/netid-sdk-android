// Copyright 2022 European netID Foundation (https://enid.foundation)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.netid.mobile.sdk.example

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.netid.mobile.sdk.api.NetIdAuthFlow
import de.netid.mobile.sdk.api.NetIdConfig
import de.netid.mobile.sdk.api.NetIdError
import de.netid.mobile.sdk.api.NetIdErrorProcess
import de.netid.mobile.sdk.api.NetIdIdentifierFetchOption
import de.netid.mobile.sdk.api.NetIdLayerStyle
import de.netid.mobile.sdk.api.NetIdService
import de.netid.mobile.sdk.api.NetIdServiceListener
import de.netid.mobile.sdk.example.databinding.ActivityMainBinding
import de.netid.mobile.sdk.model.NetIdPermissionStatus
import de.netid.mobile.sdk.model.NetIdPermissionUpdate
import de.netid.mobile.sdk.model.SubjectIdentifiers
import de.netid.mobile.sdk.model.UserInfo
import de.netid.mobile.sdk.model.permission.response.PermissionResponseStatus
import de.netid.mobile.sdk.model.permission.response.read.PermissionReadResponse
import org.json.JSONObject
import kotlin.math.max

class MainActivity : AppCompatActivity(), NetIdServiceListener, AdapterView.OnItemSelectedListener {

    private lateinit var netIdConfig: NetIdConfig

    /** Companion object for basic configuration and static parts.
     */
    companion object {
        /* Basic configuration of the [NetIdService] via [NetIdConfig] object.
         * clientId and redirectUri are mandatory, all other parameters are optional.
         * Nevertheless, we set a standard set of claims here.
         */
        private const val CLIENT_ID = "ec54097f-83f6-4bb1-86f3-f7c584c649cd"
        private const val REDIRECT_URI = "https://eunid.github.io/redirectApp"
        private const val CLAIMS = "{\"userinfo\":{\"email\": {\"essential\": true}, \"email_verified\": {\"essential\": true}}}"

        private const val INVALID_ACCESS_TOKEN = "<INSERT_EXPIRED_TOKEN_HERE>"

        private val permissionIdentifierOptionList = arrayListOf(
            PermissionIdentifierOption.Default.optionString,
            PermissionIdentifierOption.AllIdentifiers.optionString
        )

        // Using default text / icon
        private val permissionLayerConfig = null
        private val loginLayerConfig = null

        private var serviceState = ServiceState.Uninitialized

        @SuppressLint("StaticFieldLeak")
        private lateinit var binding: ActivityMainBinding

        private var bottomDialogFragment = SdkContentBottomDialogFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.activityMainStyleSpinner.onItemSelectedListener = this

        setupInitializeButton()
        setupAuthorizeButton()
        setupIdentifierOptionSpinner()
        setupUserInfoButton()
        setupPermissionManagementButtons()
        setupSetAccessTokenButton()
        setupEndSessionButton()
        updateElementsForServiceState()

        // If we have a saved state then we can restore it now
        if (savedInstanceState != null) {
            serviceState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable("serviceState", ServiceState::class.java) as ServiceState
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getSerializable("serviceState") as ServiceState
            }

            updateElementsForServiceState()
        }
    }

    override fun onRestoreInstanceState(inState: Bundle) {
        super.onRestoreInstanceState(inState)
        serviceState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            inState.getSerializable("serviceState", ServiceState::class.java) as ServiceState
        } else {
            @Suppress("DEPRECATION")
            inState.getSerializable("serviceState") as ServiceState
        }
        // Restore log messages.
        binding.activityMainLogsTextView.text = inState.getString("log", "Logs:\n\n")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("serviceState", serviceState)
        // Save log messages.
        outState.putString("log", binding.activityMainLogsTextView.text.toString())
    }

    private fun setupNetIdConfig() {
        var effectiveClaims = CLAIMS
        if (binding.activityMainCheckBoxShippingAddress.isChecked) {
            val jsonClaims = JSONObject(effectiveClaims)
            val userinfo = jsonClaims.get("userinfo") as JSONObject
            userinfo.put("shipping_address", JSONObject.NULL)
            effectiveClaims = jsonClaims.toString()
        }
        if (binding.activityMainCheckBoxBirthdate.isChecked) {
            val jsonClaims = JSONObject(effectiveClaims)
            val userinfo = jsonClaims.getJSONObject("userinfo")
            userinfo.put("birthdate", JSONObject.NULL)
            effectiveClaims = jsonClaims.toString()
        }

        netIdConfig = NetIdConfig(
            clientId = CLIENT_ID,
            redirectUri = REDIRECT_URI,
            claims = effectiveClaims,
            promptWeb = "consent",
            permissionLayerConfig = permissionLayerConfig,
            loginLayerConfig = loginLayerConfig
        )

        NetIdService.addListener(this)
    }

    private fun setupInitializeButton() {
        binding.activityMainButtonInitialize.setOnClickListener {
            it.isEnabled = false
            setupNetIdConfig()
            NetIdService.initialize(netIdConfig, applicationContext)
        }
    }

    private fun setupAuthorizeButton() {
        binding.activityMainButtonAuthorize.setOnClickListener {
            it.isEnabled = false

            val builder = AlertDialog.Builder(this)
            bottomDialogFragment = SdkContentBottomDialogFragment()
            builder.setTitle(R.string.net_id_service_choose_auth_way)
            builder.setPositiveButton(R.string.net_id_service_auth_permission) { _, _ ->
                bottomDialogFragment.sdkContentFragment = NetIdService.getAuthorizationFragment(this, NetIdAuthFlow.Permission)
                if (bottomDialogFragment.sdkContentFragment != null) {
                    bottomDialogFragment.show(supportFragmentManager, null)
                }
            }
            builder.setNegativeButton(R.string.net_id_service_auth_login) { _, _ ->
                bottomDialogFragment.sdkContentFragment = NetIdService.getAuthorizationFragment(this, NetIdAuthFlow.Login)
                if (bottomDialogFragment.sdkContentFragment != null) {
                    bottomDialogFragment.show(supportFragmentManager, null)
                }
            }
            builder.setNeutralButton(R.string.net_id_service_auth_login_permission) { _, _ ->
                bottomDialogFragment.sdkContentFragment = NetIdService.getAuthorizationFragment(this, NetIdAuthFlow.LoginPermission)
                if (bottomDialogFragment.sdkContentFragment != null) {
                    bottomDialogFragment.show(supportFragmentManager, null)
                }
            }
            builder.setOnDismissListener {
                binding.activityMainButtonAuthorize.isEnabled = true
                updateElementsForServiceState()
            }
            builder.show()
        }
    }

    private fun setupIdentifierOptionSpinner() {
        val backendVersionDataAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, permissionIdentifierOptionList)
        backendVersionDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.activityMainSpinnerBackendVersion.adapter = backendVersionDataAdapter

        val preselectedItem = SessionPreferences.loadBackendVersion(this, PermissionIdentifierOption.Default.toString())
        Log.d(javaClass.simpleName, "Preselected backend version: $preselectedItem")
        val preselectedIdentifierOption = PermissionIdentifierOption.valueOf(preselectedItem)
        val preselectedIndex = max(permissionIdentifierOptionList.indexOf(preselectedIdentifierOption.optionString), 0)
        Log.d(javaClass.simpleName, "Preselected backend version index: $preselectedIndex")
        binding.activityMainSpinnerBackendVersion.setSelection(preselectedIndex)

        binding.activityMainSpinnerBackendVersion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.i(javaClass.simpleName, "Backend version $position was selected")
                if (position < permissionIdentifierOptionList.size) {
                    val apiVersion = PermissionIdentifierOption.forOptionString(permissionIdentifierOptionList[position])
                    SessionPreferences.saveBackendVersion(this@MainActivity, apiVersion.toString())
                } else {
                    Log.e(
                        javaClass.simpleName,
                        "Selected backend version position is $position while backend version list size is " +
                            "${permissionIdentifierOptionList.size}"
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.w(javaClass.simpleName, "No backend version selected")
            }
        }
    }

    private fun setupUserInfoButton() {
        binding.activityMainButtonUserInfo.setOnClickListener {
            it.isEnabled = false
            NetIdService.fetchUserInfo(applicationContext)
        }
    }

    private fun setupPermissionManagementButtons() {
        binding.activityMainButtonPermissionRead.setOnClickListener {
            it.isEnabled = false

            val version = SessionPreferences.loadBackendVersion(this, PermissionIdentifierOption.Default.toString())
            val apiVersion = PermissionIdentifierOption.valueOf(version)
            if (apiVersion == PermissionIdentifierOption.AllIdentifiers) {
                NetIdService.fetchPermissions(
                    applicationContext,
                    fetchOptions = setOf(
                        NetIdIdentifierFetchOption.TagProtocolIdentifier,
                        NetIdIdentifierFetchOption.SynchronizationIdentifier,
                        NetIdIdentifierFetchOption.EncryptedTagProtocolIdentifier
                    )
                )
            } else {
                NetIdService.fetchPermissions(applicationContext)
            }
        }

        binding.activityMainButtonPermissionWrite.setOnClickListener {
            it.isEnabled = false

            // these values are only for demonstration purposes
            val permission = NetIdPermissionUpdate(
                NetIdPermissionStatus.VALID,
                "CPdfZIAPdfZIACnABCDECbCkAP_AAAAAAAYgIzJd9D7dbXFDefx_SPt0OYwW0NBXCuQCChSAA2AFVAOQcLQA02EaMATAhiACEQIAolIBAAEEHAFEAECQQIAEAAHsAgSEhAAKIAJEEBEQAAIQAAoKAAAAAAAIgAABoASAmBiQS5bmRUCAOIAQRgBIgggBCIADAgMBBEAIABgIAIIIgSgAAQAAAKIAAAAAARAAAASGgFABcAEMAPwAgoBaQEiAJ2AUiAxgBnwqASAEMAJgAXABHAEcALSAkEBeYDPh0EIABYAFQAMgAcgA-AEAALgAZAA0AB4AD6AIYAigBMACfAFwAXQAxABmADeAHMAPwAhgBLACYAE0AKMAUoAsQBbgDDAGiAPaAfgB-gEDAIoARaAjgCOgEpALEAWmAuYC6gF5AMUAbQA3ABxADnAHUAPQAi8BIICRAE7AKHAXmAwYBjADJAGVAMsAZmAz4BrADiwHjgPrAg0BDkhAbAAWABkAFwAQwAmABcADEAGYAN4AjgBSgCxAIoARwAlIBaQC5gGKANoAc4A6gB6AEggJEAScAz4B45KBAAAgABYAGQAOAAfAB4AEQAJgAXAAxABmADaAIYARwAowBSgC3AH4ARwAk4BaQC6gGKANwAdQBF4CRAF5gMsAZ8A1gCGoSBeAAgABYAFQAMgAcgA8AEAAMgAaAA8gCGAIoATAAngBvADmAH4AQgAhgBHACWAE0AKUAW4AwwB7QD8AP0AgYBFICNAI4ASkAuYBigDaAG4AOIAegBIgCdgFDgKRAXmAwYBkgDPoGsAayA4IB44EOREAYAQwA_AEiAJ2AUiAz4ZAHACGAEwARwBHAEnALzAZ8UgXAALAAqABkADkAHwAgABkADQAHkAQwBFACYAE8AKQAYgAzABzAD8AIYAUYApQBYgC3AGjAPwA_QCLQEcAR0AlIBcwC8gGKANoAbgA9ACLwEiAJOATsAocBeYDGAGSAMsAZ9A1gDWQHBAPHAhm.f_gAAAAAAsgA"
            )

            val version = SessionPreferences.loadBackendVersion(this, PermissionIdentifierOption.Default.toString())
            val apiVersion = PermissionIdentifierOption.valueOf(version)
            if (apiVersion == PermissionIdentifierOption.AllIdentifiers) {
                NetIdService.updatePermission(
                    applicationContext,
                    permission,
                    fetchOptions = setOf(
                        NetIdIdentifierFetchOption.TagProtocolIdentifier,
                        NetIdIdentifierFetchOption.SynchronizationIdentifier,
                        NetIdIdentifierFetchOption.EncryptedTagProtocolIdentifier
                    )
                )
            } else {
                NetIdService.updatePermission(applicationContext, permission)
            }
        }
    }

    private fun setupSetAccessTokenButton() {
        binding.activityMainButtonSetAccessToken.setOnClickListener {
            NetIdService.setAccessToken(INVALID_ACCESS_TOKEN)
        }
    }

    private fun setupEndSessionButton() {
        binding.activityMainButtonEndSession.setOnClickListener {
            it.isEnabled = false
            appendLog("netID service session finished successfully")
            NetIdService.endSession()
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
        val isUninitialized = serviceState == ServiceState.Uninitialized || serviceState == ServiceState.InitializationFailed
        val isNotAuthorized = serviceState == ServiceState.InitializationSuccessful || serviceState == ServiceState.AuthorizationFailed
        val isAuthorized = serviceState == ServiceState.AuthorizationSuccessful || serviceState == ServiceState.UserInfoFailed ||
            serviceState == ServiceState.UserInfoSuccessful || serviceState == ServiceState.PermissionWriteSuccessful ||
            serviceState == ServiceState.PermissionWriteFailed || serviceState == ServiceState.PermissionReadFailed ||
            serviceState == ServiceState.PermissionReadSuccessful

        binding.activityMainButtonInitialize.isEnabled = isUninitialized
        binding.activityMainButtonAuthorize.isEnabled = isNotAuthorized
        binding.activityMainButtonUserInfo.isEnabled = isAuthorized
        binding.activityMainButtonEndSession.isEnabled = isAuthorized
        binding.activityMainButtonPermissionWrite.isEnabled = isAuthorized
        binding.activityMainButtonPermissionRead.isEnabled = isAuthorized
        binding.activityMainButtonSetAccessToken.isEnabled = isAuthorized
        binding.activityMainCheckBoxShippingAddress.isEnabled = isUninitialized
        binding.activityMainCheckBoxBirthdate.isEnabled = isUninitialized

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
        bottomDialogFragment.dismiss()
        updateElementsForServiceState()
    }

    override fun onAuthenticationFinishedWithError(error: NetIdError) {
        if (error.msg?.isNotEmpty() == true) {
            appendLog("netID service authorization failed: ${error.code}, ${error.process}, ${error.msg}")
        } else {
            appendLog("netID service authorization failed: ${error.code}, ${error.process}")
        }
        serviceState = ServiceState.AuthorizationFailed
        bottomDialogFragment.dismiss()
        updateElementsForServiceState()
    }

    override fun onUserInfoFinished(userInfo: UserInfo) {
        appendLog("netID service user info - fetch finished successfully: $userInfo")
        serviceState = ServiceState.UserInfoSuccessful
        updateElementsForServiceState()
    }

    override fun onUserInfoFetchedWithError(error: NetIdError) {
        if (error.msg?.isNotEmpty() == true) {
            appendLog("netID service user info - fetch failed: ${error.code}, ${error.process}, ${error.msg}")
        } else {
            appendLog("netID service user info - fetch failed: ${error.code}, ${error.process}")
        }
        serviceState = ServiceState.UserInfoFailed
        updateElementsForServiceState()
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
        appendLog("netID service user canceled authentication in process: ${error.process}")
        if (error.msg?.isNotEmpty() == true) {
            appendLog("original error message: ${error.msg}")
        }
        serviceState = when (error.process) {
            NetIdErrorProcess.Configuration -> ServiceState.InitializationFailed
            NetIdErrorProcess.Authentication -> ServiceState.AuthorizationFailed
            NetIdErrorProcess.UserInfo -> ServiceState.UserInfoFailed
            else -> ServiceState.Uninitialized
        }
        bottomDialogFragment.dismiss()
        updateElementsForServiceState()
    }

    override fun onPermissionUpdateFinishedWithError(statusCode: PermissionResponseStatus, error: NetIdError) {
        when (statusCode) {
            PermissionResponseStatus.NO_TOKEN ->
                // no token was passed (handled by SDK should not happen)
                appendLog("No bearer token in request available.")

            PermissionResponseStatus.TOKEN_ERROR ->
                // current token expired / is invalid
                appendLog("Token error - token refresh / reauthorization necessary")

            PermissionResponseStatus.TPID_EXISTENCE_ERROR ->
                // netID Account was deleted
                appendLog("netID Account was deleted")

            PermissionResponseStatus.TAPP_NOT_ALLOWED ->
                // Invalid configuration of client
                appendLog("Client not authorized to use permission management")

            PermissionResponseStatus.PERMISSION_PARAMETERS_ERROR ->
                // Invalid parameter payload
                appendLog("Syntactic or semantic error in a permission")

            PermissionResponseStatus.NO_PERMISSIONS ->
                // No permission parameter given
                appendLog("Parameters are missing. At least one permission must be set.")

            PermissionResponseStatus.NO_REQUEST_BODY ->
                // Request body missing
                appendLog("Required request body is missing")

            PermissionResponseStatus.JSON_PARSE_ERROR ->
                // Error parsing JSON body
                appendLog("Invalid JSON body, parse error")

            else ->
                appendLog("netID service permission - update failed with error: ${error.code}")
        }
        if (error.msg?.isNotEmpty() == true) {
            appendLog("original error message: ${error.msg}")
        }
        serviceState = ServiceState.PermissionWriteFailed
        updateElementsForServiceState()
    }

    override fun onPermissionFetchFinishedWithError(statusCode: PermissionResponseStatus, error: NetIdError) {
        when (statusCode) {
            PermissionResponseStatus.NO_TOKEN ->
                // no token was passed (handled by SDK should not happen)
                appendLog("No bearer token in request available.")

            PermissionResponseStatus.TOKEN_ERROR ->
                // current token expired / is invalid
                appendLog("Token error - token refresh / reauthorization necessary")

            PermissionResponseStatus.TPID_EXISTENCE_ERROR ->
                // netID Account was deleted
                appendLog("netID Account was deleted")

            PermissionResponseStatus.TAPP_NOT_ALLOWED ->
                // Invalid configuration of client
                appendLog("Client not authorized to use permission management")

            PermissionResponseStatus.PERMISSIONS_NOT_FOUND ->
                // Missing permissions
                appendLog("Permissions for tpid not found")

            else ->
                appendLog("netID service permission - fetch failed with error: ${error.code}")
        }
        if (error.msg?.isNotEmpty() == true) {
            appendLog("original error message: ${error.msg}")
        }
        serviceState = ServiceState.PermissionReadFailed
        updateElementsForServiceState()
    }

    override fun onPermissionFetchFinished(permissions: PermissionReadResponse) {
        appendLog("netID service permission - fetch finished successfully")

        when (permissions.statusCode) {
            PermissionResponseStatus.PERMISSIONS_FOUND ->
                // Response contains existing permission
                appendLog("Permissions: $permissions")

            PermissionResponseStatus.PERMISSIONS_NOT_FOUND ->
                // No existing permission found
                appendLog("No permissions found")

            else ->
                appendLog("This should not happen")
        }
        serviceState = ServiceState.PermissionReadSuccessful
        updateElementsForServiceState()
    }

    override fun onPermissionUpdateFinished(subjectIdentifiers: SubjectIdentifiers) {
        appendLog("netID service permission - update finished successfully.")
        appendLog("Returned: $subjectIdentifiers")
        serviceState = ServiceState.PermissionWriteSuccessful
        updateElementsForServiceState()
    }

    // OnItemSelectedListener
    override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, id: Long) {
        NetIdService.setLayerStyle(NetIdLayerStyle.entries[position])
    }

    override fun onNothingSelected(arg0: AdapterView<*>?) {
        // TODO Auto-generated method stub
    }
}
