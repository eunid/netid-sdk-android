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
import de.netid.mobile.sdk.api.NetIdAuthFlow
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

        binding.buttonLoginContinue.setOnClickListener {
            authorizationIntent = NetIdService.authIntentForFlow(flow, it.context)
            resultLauncher.launch(authorizationIntent)
        }

        if (continueText.isNotEmpty()) {
            binding.buttonLoginContinue.text = continueText
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}