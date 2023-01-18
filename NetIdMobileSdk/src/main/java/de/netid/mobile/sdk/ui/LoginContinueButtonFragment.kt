package de.netid.mobile.sdk.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.api.NetIdAuthFlow
import de.netid.mobile.sdk.api.NetIdButtonStyle
import de.netid.mobile.sdk.api.NetIdService
import de.netid.mobile.sdk.databinding.LoginContinueButtonBinding
import de.netid.mobile.sdk.model.AppIdentifier

class LoginContinueButtonFragment(
    private val listener: AuthorizationFragmentListener,
    private val continueText: String = "",
    private val flow: NetIdAuthFlow = NetIdAuthFlow.Login
): Fragment() {

    private var _binding: LoginContinueButtonBinding? = null

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
        _binding = LoginContinueButtonBinding.inflate(inflater, container, false)
        retainInstance = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setButtonStyle(NetIdService.getButtonStyle())

        binding.buttonLoginContinue.setOnClickListener {
            authorizationIntent = NetIdService.authIntentForFlow(flow, it.context)
            resultLauncher.launch(authorizationIntent)
        }

        if (continueText.isNotEmpty()) {
            binding.buttonLoginContinue.text = continueText
        }
    }

    /**
     * Sets the style of the button
     * @param buttonStyle Button style to set.
     */
    fun setButtonStyle(buttonStyle: NetIdButtonStyle) {
        var netIdLogoResource = R.drawable.ic_netid_logo_small
        var buttonBackgroundResource = R.color.authorization_agree_button_color
        var buttonForegroundResource = R.color.authorization_agree_text_color
        var buttonOutlineResource = R.color.authorization_close_button_color
        var buttonStrokeWidthResource = R.dimen.authorization_close_button_stroke_width

        when (NetIdService.getButtonStyle()) {
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
                buttonOutlineResource = R.color.authorization_close_button_color
                buttonStrokeWidthResource = R.dimen.authorization_close_button_stroke_width
            }
        }

        binding.buttonLoginContinue.setTextColor(resources.getColor(buttonForegroundResource))
        binding.buttonLoginContinue.setBackgroundColor(resources.getColor(buttonBackgroundResource))
        binding.buttonLoginContinue.setStrokeColorResource(buttonOutlineResource)
        binding.buttonLoginContinue.icon = resources.getDrawable(netIdLogoResource)
        binding.buttonLoginContinue.setStrokeWidthResource(buttonStrokeWidthResource)
    }


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}