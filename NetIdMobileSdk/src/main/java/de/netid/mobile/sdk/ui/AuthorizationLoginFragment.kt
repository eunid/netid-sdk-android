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

package de.netid.mobile.sdk.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.api.NetIdLayerStyle
import de.netid.mobile.sdk.api.NetIdService
import de.netid.mobile.sdk.databinding.FragmentAuthorizationLoginBinding
import de.netid.mobile.sdk.model.AppIdentifier

class AuthorizationLoginFragment(
    private val listener: AuthorizationFragmentListener,
    private val appIdentifiers: List<AppIdentifier>,
    private val authorizationIntent: Intent,
    private val headlineText: String = "",
    private val loginText: String = "",
    private val continueText: String = ""
) : Fragment() {
    private var _binding: FragmentAuthorizationLoginBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            listener.onAuthenticationFinished(result.data)
        } else {
            listener.onAuthenticationFailed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthorizationLoginBinding.inflate(inflater, container, false)
        retainInstance = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureStandardButton()
        configureIdAppButtons()
        if (headlineText.isNotEmpty()) {
            binding.fragmentAuthorizationTitleTextView.text = headlineText
        }
    }

    /**
     * Internal helper function that configures a standard button.
     * Actually, this button just closes the login dialog.
     */
    private fun configureStandardButton() {
        var netIdLogoResource = R.drawable.ic_netid_logo_small
        var buttonBackgroundResource = R.color.authorization_agree_button_color
        var buttonForegroundResource = R.color.authorization_agree_text_color
        var buttonOutlineResource = R.color.authorization_close_button_color

        when (NetIdService.getLayerStyle()) {
            NetIdLayerStyle.Outline -> {
                netIdLogoResource = R.drawable.ic_netid_logo_small
                buttonBackgroundResource = R.color.outline_background_color
                buttonForegroundResource = R.color.outline_text_color
                buttonOutlineResource = R.color.outline_outline_color
            }
            else -> {
                netIdLogoResource = R.drawable.ic_netid_logo_small
                buttonBackgroundResource = R.color.authorization_agree_button_color
                buttonForegroundResource = R.color.authorization_agree_text_color
                buttonOutlineResource = R.color.authorization_close_button_color
            }
        }

        binding.fragmentAuthorizationButtonAgreeAndContinue.setTextColor(resources.getColor(buttonForegroundResource))
        binding.fragmentAuthorizationButtonAgreeAndContinue.setBackgroundColor(resources.getColor(buttonBackgroundResource))
        binding.fragmentAuthorizationButtonAgreeAndContinue.setStrokeColorResource(buttonOutlineResource)
        binding.fragmentAuthorizationButtonAgreeAndContinue.icon = resources.getDrawable(netIdLogoResource)

        binding.fragmentAuthorizationButtonCloseContinue.setOnClickListener {
            listener.onCloseClicked()
        }
    }

    /**
     * Internal helper function that configures buttons for id apps.
     * If there are no id apps installed, a standard button for initializing the app2web-flow
     * is configured instead.
     */
    private fun configureIdAppButtons() {
        // If there are no apps installed, display a standard button to enable app2web flow
        if (appIdentifiers.isEmpty()) {
            binding.fragmentAuthorizationTitleTextView.visibility = View.GONE
            binding.fragmentAuthorizationButtonAgreeAndContinue.visibility = View.VISIBLE
            binding.fragmentAuthorizationButtonAgreeAndContinue.setOnClickListener {
                resultLauncher.launch(authorizationIntent)
            }
        }

        appIdentifiers.forEachIndexed { index, appIdentifier ->
            val appButton = createButton(appIdentifier)

            binding.fragmentAuthorizationButtonContainerLayout.addView(appButton, index)
        }
        if (continueText.isNotEmpty()) {
            binding.fragmentAuthorizationButtonCloseContinue.text = continueText
        }
    }

    /**
     * Internal helper function that creates and styles a button for an id app.
     * @param appIdentifier AppIdentifier holds all information to configure the button
     * @return button
     */
    public fun createButton(appIdentifier: AppIdentifier): MaterialButton {
        var netIdLogoResource = appIdentifier.typeFaceIcon
        var buttonBackgroundResource = appIdentifier.backgroundColor
        var buttonForegroundResource = Color.parseColor(appIdentifier.foregroundColor)
        var strokeWidth = R.dimen.authorization_close_button_stroke_zero_width

        when (NetIdService.getLayerStyle()) {
            NetIdLayerStyle.Outline -> {
                netIdLogoResource = appIdentifier.typeFaceIcon + "_outline"
                buttonBackgroundResource = appIdentifier.backgroundColorOutline
                buttonForegroundResource = resources.getColor(R.color.authorization_agree_text_color)
                strokeWidth = R.dimen.authorization_close_button_stroke_width
            }
            else -> {
                netIdLogoResource = appIdentifier.typeFaceIcon
                buttonBackgroundResource = appIdentifier.backgroundColor
                buttonForegroundResource = Color.parseColor(appIdentifier.foregroundColor)
                strokeWidth = R.dimen.authorization_close_button_stroke_zero_width
            }
        }

        val appButton = layoutInflater.inflate(R.layout.account_provider_app_button, null, false) as MaterialButton

        val resourceId =
            context?.resources?.getIdentifier(netIdLogoResource, "drawable", requireContext().packageName)
        appButton.icon = resourceId?.let {
            ResourcesCompat.getDrawable(
                requireContext().resources,
                it,
                null
            )
        }

        val loginString = if (loginText.isEmpty()) {
            getString(R.string.authorization_login_continue, appIdentifier.name)
        } else {
            String.format(loginText, appIdentifier.name)
        }
        appButton.text = loginString.uppercase()
        appButton.setTextColor(buttonForegroundResource)
        appButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(buttonBackgroundResource))
        appButton.setStrokeWidthResource(strokeWidth)

        appButton.setOnClickListener {
            openApp(appIdentifier)
        }

        return appButton
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /**
     * Open an id app via a VerifiedAppLink.
     * @param appIdentifier AppIdentifier of the app to be opened.
     */
    private fun openApp(appIdentifier: AppIdentifier) {
        authorizationIntent.extras?.apply {
            val authIntent = getParcelable<Intent>("authIntent") ?: return@apply
            val authUri = authIntent.data as Uri
            val uri = authUri.toString().replaceBefore("?", appIdentifier.android.verifiedAppLink)
            authIntent.setPackage(appIdentifier.android.applicationId)
            authIntent.data = Uri.parse(uri)
            putParcelable("authIntent", authIntent)
        }
        resultLauncher.launch(authorizationIntent)
    }
}
