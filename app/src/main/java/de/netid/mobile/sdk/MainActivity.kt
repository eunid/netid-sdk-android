package de.netid.mobile.sdk

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.netid.mobile.sdk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var serviceState = ServiceState.Uninitialized

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitializeButton()
        setupAuthorizeButton()
        setupUserInfoButton()
        setupEndSessionButton()

        updateElementsForServiceState()
    }

    private fun setupInitializeButton() {
        binding.activityMainButtonInitialize.setOnClickListener {
            it.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    appendLog("Net ID service initialized successfully")
                    serviceState = ServiceState.InitializationSuccessful
                    updateElementsForServiceState()
                }, 500
            )
        }
    }

    private fun setupAuthorizeButton() {
        binding.activityMainButtonAuthorize.setOnClickListener {
            it.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    appendLog("Net ID service authorized successfully")
                    serviceState = ServiceState.AuthorizationSuccessful
                    updateElementsForServiceState()
                }, 500
            )
        }
    }

    private fun setupUserInfoButton() {
        binding.activityMainButtonUserInfo.setOnClickListener {
            it.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    appendLog("Fetched user info successfully")
                    serviceState = ServiceState.UserInfoSuccessful
                    updateElementsForServiceState()
                }, 500
            )
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
        val isUninitialized = serviceState == ServiceState.Uninitialized || serviceState == ServiceState.InitializationFailed
        val isNotAuthorized = serviceState == ServiceState.InitializationSuccessful || serviceState == ServiceState.AuthorizationFailed
        val isAuthorized = serviceState == ServiceState.AuthorizationSuccessful || serviceState == ServiceState.UserInfoFailed
                || serviceState == ServiceState.UserInfoSuccessful

        binding.activityMainButtonInitialize.isEnabled = isUninitialized
        binding.activityMainButtonAuthorize.isEnabled = isNotAuthorized
        binding.activityMainButtonUserInfo.isEnabled = isAuthorized
        binding.activityMainButtonEndSession.isEnabled = isAuthorized

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
}
