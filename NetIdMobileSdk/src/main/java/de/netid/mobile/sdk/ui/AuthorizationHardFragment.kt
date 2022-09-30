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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.api.NetIdConfig
import de.netid.mobile.sdk.databinding.FragmentAuthorizationHardBinding
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.util.UriUtil
import java.net.URL

class AuthorizationHardFragment(
    private val listener: AuthorizationFragmentListener,
    private val appIdentifiers: List<AppIdentifier>,
    private val authorizationIntent: Intent
) : Fragment() {
    companion object {
        private const val netIdScheme = "scheme"
    }

    private var _binding: FragmentAuthorizationHardBinding? = null

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
        _binding = FragmentAuthorizationHardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStandardButtons()
        setupAppButtons()
    }

    private fun setupStandardButtons() {
        var currentAppName = "App"
        if (context?.applicationInfo?.name.toString().uppercase() != "NULL") {
            currentAppName = context?.applicationInfo?.name.toString()
        }
        val continueString = getString(R.string.authorization_hard_continue, currentAppName)
        binding.fragmentAuthorizationButtonClose.text = continueString.uppercase()
        binding.fragmentAuthorizationButtonClose.setOnClickListener {
            listener.onCloseClicked()
        }
    }

    private fun setupAppButtons() {
        if (appIdentifiers.size == 1) {
            context?.let { context ->
                val resourceId =
                    context.resources?.getIdentifier(appIdentifiers[0].typeFaceIcon, "drawable", context.opPackageName)
                binding.fragmentAuthorizationBrandLogoImageView.setImageDrawable(
                    resourceId?.let {
                        ResourcesCompat.getDrawable(
                            context.resources,
                            it,
                            null
                        )
                    }
                )
            }
        } else {
            binding.fragmentAuthorizationBrandLogoImageView.isVisible = false
        }

        appIdentifiers.forEachIndexed { index, appIdentifier ->
            val appButton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle)
            val continueString = getString(R.string.authorization_hard_continue, appIdentifier.name)
            appButton.text = continueString.uppercase()
            appButton.setTextColor(Color.parseColor(appIdentifier.foregroundColor))
            appButton.isAllCaps = false
            appButton.typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
            appButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.authorization_button_text_size))
            appButton.setCornerRadiusResource(R.dimen.authorization_button_corner_radius)
            appButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(appIdentifier.backgroundColor))

            val letterSpacingValue = TypedValue()
            resources.getValue(R.dimen.authorization_button_letter_spacing, letterSpacingValue, true)
            appButton.letterSpacing = letterSpacingValue.float

            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            appButton.layoutParams = layoutParams

            appButton.setOnClickListener {
                resultLauncher.launch(authorizationIntent)
                listener.onAppButtonClicked(appIdentifier)
                openApp(appIdentifier.android.verifiedAppLink)
            }

            binding.fragmentAuthorizationButtonContainerLayout.addView(appButton, index)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun openApp(verifiedAppLink: String) {
        val authIntent = authorizationIntent.extras?.get("authIntent")as Intent
        val authUri = authIntent.data as Uri
        val uri = authUri.toString().replaceBefore("?", verifiedAppLink)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
//        intent?.putExtra(netIdScheme, context?.applicationInfo?.packageName)
        intent.let {
            context?.startActivity(intent)
        }
    }
}
