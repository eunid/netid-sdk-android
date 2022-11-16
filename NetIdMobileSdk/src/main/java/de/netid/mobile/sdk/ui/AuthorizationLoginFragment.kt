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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.api.NetIdAuthFlow
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
    companion object {
        private const val netIdScheme = "scheme"
    }

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStandardButtons()
        setupAppButtons()
        if (headlineText.isNotEmpty()) {
            binding.fragmentAuthorizationTitleTextView.text = headlineText
        }
    }

    private fun setupStandardButtons() {
        binding.fragmentAuthorizationButtonClose.setOnClickListener {
            listener.onCloseClicked()
        }
        binding.fragmentAuthorizationButtonAgreeAndContinue.setOnClickListener {
            resultLauncher.launch(authorizationIntent)
        }
    }

    private fun setupAppButtons() {
        // If there are no apps installed, display a standard button to enable app2web flow
        if (appIdentifiers.isEmpty()) {
            binding.fragmentAuthorizationButtonAgreeAndContinue.visibility = View.VISIBLE
        }

        appIdentifiers.forEachIndexed { index, appIdentifier ->
            val appButton = createButton(appIdentifier)

            binding.fragmentAuthorizationButtonContainerLayout.addView(appButton, index)
        }
        if (continueText.isNotEmpty()) {
            binding.fragmentAuthorizationButtonClose.text = continueText
        }
    }

    private fun createButton(appIdentifier: AppIdentifier): MaterialButton {
        val appButton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle)
        val continueString = if (loginText.isEmpty()) {
            getString(R.string.authorization_login_continue, appIdentifier.name)
        } else {
            String.format(loginText, appIdentifier.name)
        }
        val resourceId =
            context?.resources?.getIdentifier(appIdentifiers[0].typeFaceIcon, "drawable", requireContext().opPackageName)
        appButton.icon = resourceId?.let {
            ResourcesCompat.getDrawable(
                requireContext().resources,
                it,
                null
            )
        }
        appButton.iconTint = ColorStateList.valueOf(Color.parseColor(appIdentifier.foregroundColor))
        appButton.iconPadding = -100
        appButton.text = continueString.uppercase()
        appButton.setTextColor(Color.parseColor(appIdentifier.foregroundColor))
        appButton.isAllCaps = true
        appButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.authorization_button_text_size))
        appButton.setCornerRadiusResource(R.dimen.authorization_button_corner_radius)
        appButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(appIdentifier.backgroundColor))

        val letterSpacingValue = TypedValue()
        resources.getValue(R.dimen.authorization_button_letter_spacing, letterSpacingValue, true)
        appButton.letterSpacing = letterSpacingValue.float

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        appButton.layoutParams = layoutParams

        appButton.setOnClickListener {
            listener.onAppButtonClicked(appIdentifier)
            openApp(appIdentifier.android.verifiedAppLink)
        }

        return appButton
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun openApp(verifiedAppLink: String) {
        authorizationIntent.extras?.apply {
            val authIntent = getParcelable<Intent>("authIntent") ?: return@apply
            val authUri = authIntent.data as Uri
            val uri = authUri.toString().replaceBefore("?", verifiedAppLink)
            authIntent.setPackage(null)
            authIntent.data = Uri.parse(uri)
            putParcelable("authIntent", authIntent)
        }
        resultLauncher.launch(authorizationIntent)
    }
}