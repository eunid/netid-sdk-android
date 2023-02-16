package de.netid.mobile.sdk.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.api.NetIdAuthFlow
import de.netid.mobile.sdk.api.NetIdButtonStyle
import de.netid.mobile.sdk.api.NetIdService
import de.netid.mobile.sdk.databinding.AccountProviderAppButtonBinding
import de.netid.mobile.sdk.model.AppIdentifier

class AccountProviderAppButtonFragment(
    private val listener: AuthorizationFragmentListener,
    private val appIdentifier: AppIdentifier,
    private val flow: NetIdAuthFlow,
    private val continueText: String
): Fragment() {

    private var _binding: AccountProviderAppButtonBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var authorizationIntent: Intent? = null

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
        _binding = AccountProviderAppButtonBinding.inflate(inflater, container, false)
        retainInstance = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setButtonStyle(NetIdService.getButtonStyle())

        when (flow) {
            NetIdAuthFlow.Permission -> binding.buttonApp.text = String.format(getString(R.string.authorization_permission_continue_button)).uppercase()
            NetIdAuthFlow.Login -> binding.buttonApp.text = String.format(getString(R.string.authorization_login_continue), appIdentifier.name).uppercase()
            NetIdAuthFlow.LoginPermission -> binding.buttonApp.text = String.format(getString(R.string.authorization_login_continue), appIdentifier.name).uppercase()
        }
        if (continueText.isNotEmpty()) {
            binding.buttonApp.text = continueText
        }

        binding.buttonApp.setOnClickListener {
            authorizationIntent = NetIdService.authIntentForFlow(flow, it.context)
            openApp(appIdentifier)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /**
     * Sets the style of the button
     * @param buttonStyle Button style to set.
     */
    fun setButtonStyle(buttonStyle: NetIdButtonStyle) {
        var netIdLogoResource = R.drawable.ic_netid_logo_small
        var buttonBackgroundResource = R.color.authorization_agree_button_color
        var buttonForegroundResource = R.color.authorization_agree_text_color
        var buttonOutlineResource = R.color.authorization_agree_outline_color
        var buttonStrokeWidthResource = R.dimen.authorization_close_button_stroke_width

        when (buttonStyle) {
            NetIdButtonStyle.GreenSolid -> {
                netIdLogoResource = R.drawable.ic_netid_logo_button_white
                buttonBackgroundResource = R.color.green_background_color
                buttonForegroundResource = R.color.green_text_color
                buttonOutlineResource = R.color.green_outline_color
                buttonStrokeWidthResource = R.dimen.authorization_close_button_stroke_zero_width
            }
            NetIdButtonStyle.GrayOutline -> {
                netIdLogoResource = R.drawable.ic_netid_logo_small
                buttonBackgroundResource = R.color.outline_background_color
                buttonForegroundResource = R.color.outline_text_color
                buttonOutlineResource = R.color.outline_outline_color
                buttonStrokeWidthResource = R.dimen.authorization_close_button_stroke_width
            }
            else -> {
                netIdLogoResource = R.drawable.ic_netid_logo_small
                buttonBackgroundResource = R.color.authorization_agree_button_color
                buttonForegroundResource = R.color.authorization_agree_text_color
                buttonOutlineResource = R.color.authorization_agree_outline_color
                buttonStrokeWidthResource = R.dimen.authorization_close_button_stroke_width
            }
        }

        binding.buttonApp.setTextColor(resources.getColor(buttonForegroundResource, null))
        binding.buttonApp.setBackgroundColor(resources.getColor(buttonBackgroundResource, null))
        binding.buttonApp.setStrokeColorResource(buttonOutlineResource)
        binding.buttonApp.icon = resources.getDrawable(netIdLogoResource, null)
        binding.buttonApp.setStrokeWidthResource(buttonStrokeWidthResource)
    }

    /**
     * Open an id app via a VerifiedAppLink.
     * @param appIdentifier AppIdentifier of the app to be opened.
     */
    private fun openApp(appIdentifier: AppIdentifier) {
        authorizationIntent?.extras?.apply {
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