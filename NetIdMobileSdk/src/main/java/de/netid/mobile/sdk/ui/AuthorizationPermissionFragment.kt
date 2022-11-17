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
import android.net.Uri
import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.databinding.FragmentAuthorizationPermissionBinding
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.ui.adapter.AuthorizationAppListAdapter
import de.netid.mobile.sdk.ui.adapter.AuthorizationAppListAdapterListener


class AuthorizationPermissionFragment(
    private val listener: AuthorizationFragmentListener,
    private val appIdentifiers: MutableList<AppIdentifier> = mutableListOf(),
    private val authorizationIntent: Intent,
    private val logoId: String = "",
    private val headlineText: String = "",
    private val legalText: String = "",
    private val continueText: String = ""
) : Fragment(), AuthorizationAppListAdapterListener {

    private var _binding: FragmentAuthorizationPermissionBinding? = null

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
        _binding = FragmentAuthorizationPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStandardButtons()
        setupAppButtons()
        if (logoId.isNotEmpty()) {
            var logo = context?.resources?.getIdentifier(logoId, "drawable", context?.opPackageName)
                ?.let { context?.getDrawable(it) }
            if (logo != null) {
                binding.fragmentAuthorizationLogoImageView.setImageDrawable(logo)
            }
        }
        if (headlineText.isNotEmpty()) {
            binding.fragmentAuthorizationTitleTextView.text = headlineText
        }
        if (continueText.isNotEmpty()) {
            binding.fragmentAuthorizationButtonAgreeAndContinue.text = continueText
        }
    }

    private fun setupStandardButtons() {
        binding.fragmentAuthorizationButtonAgreeAndContinue.text = getString(R.string.authorization_permission_agree_and_continue_with_net_id).uppercase()
        binding.fragmentAuthorizationButtonAgreeAndContinue.setOnClickListener {
            var adapter = binding.fragmentAuthorizationAppCellContainer.adapter as? AuthorizationAppListAdapter
            // If we only have one app or the user did not make changes to the default, use the standard one.
            if (adapter == null) adapter = context?.let { AuthorizationAppListAdapter(it, appIdentifiers) }
            if ((adapter != null) && (adapter.selectedPosition != -1) && (appIdentifiers.size != 0)) {
                adapter.getItem(adapter.selectedPosition).android.verifiedAppLink.let { verifiedAppLink ->
                    openApp(verifiedAppLink)
                }
            } else {
                resultLauncher.launch(authorizationIntent)
            }
        }
        binding.fragmentAuthorizationButtonClose.setOnClickListener {
            listener.onCloseClicked()
        }
    }

    private fun setupAppButtons() {
        val netIdString = getString(R.string.authorization_permission_net_id)
        val chooseString = getString(R.string.authorization_permission_choose_account_provider)
        val legalInfoTextView = binding.fragmentAuthorizationLegalInfoTextView

        when (appIdentifiers.size) {
            0 -> legalInfoTextView.text = if (legalText.isEmpty()) {
                getString(R.string.authorization_permission_legal_info, netIdString) + getString(R.string.authorization_permission_legal_info_fixed, netIdString, "")
            } else {
                String.format(legalText, netIdString) + getString(R.string.authorization_permission_legal_info_fixed, netIdString, "")
            }
            1 -> legalInfoTextView.text = if (legalText.isEmpty()) {
                getString(R.string.authorization_permission_legal_info, appIdentifiers[0].name) + getString(R.string.authorization_permission_legal_info_fixed, appIdentifiers[0].name, "")
            } else {
                String.format(legalText, appIdentifiers[0].name) + getString(R.string.authorization_permission_legal_info_fixed, appIdentifiers[0].name, "")
            }
            else -> {
                legalInfoTextView.text = if (legalText.isEmpty()) {
                    getString(
                        R.string.authorization_permission_legal_info,
                        appIdentifiers[0].name
                    ) + getString(R.string.authorization_permission_legal_info_fixed, appIdentifiers[0].name, chooseString)
                } else {
                    String.format(legalText, appIdentifiers[0].name) + getString(R.string.authorization_permission_legal_info_fixed, appIdentifiers[0].name, chooseString)
                }
                // We only need to add the link to the text, if we have more than one id app installed.
                // Otherwise, we leave the text as it is.
                legalInfoTextView.createAccountProviderSelection(
                    Pair(chooseString, View.OnClickListener {
                        val listView: ListView = binding.fragmentAuthorizationAppCellContainer
                        val listAdapter =
                            context?.let { AuthorizationAppListAdapter(it, appIdentifiers) }
                        listAdapter?.listener = this
                        listView.adapter = listAdapter
                    }),
                )
            }
        }
        // Enable Links for clickable spans
        legalInfoTextView.movementMethod = LinkMovementMethod.getInstance()
        // Create Link to Privacy Center
        legalInfoTextView.createLinks(Pair(
            getString(R.string.authorization_permission_privacycenter_match),
            getString(R.string.authorization_permission_privacycenter_link)))
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

    private fun TextView.createAccountProviderSelection(link: Pair<String, View.OnClickListener>) {
        val spannableString = this.text as Spannable
        val startIndexOfLink: Int = spannableString.toString().indexOf(link.first)

        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.isUnderlineText = true
            }

            override fun onClick(view: View) {
                Selection.setSelection((view as TextView).text as Spannable, 0)
                view.invalidate()
                link.second.onClick(view)
            }
        }

        if (startIndexOfLink != -1) {
            spannableString.setSpan(
                clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Searches for occurrences of a string within the TextView and
     * creates URLSpans pointing to the provided URL. Expects android:bufferType="spannable"
     *
     * @param links pairs of a string to be matched and URL to be linked to it using a URLSpan
     */
    private fun TextView.createLinks(vararg links: Pair<String,String>) {
        val spannable = this.text as Spannable
        var startIndexOfLink: Int

        for (link in links) {
            startIndexOfLink = spannable.toString().indexOf(link.first)
            //Loop over all occurrences
            while (startIndexOfLink != -1) {
                //Create URLSpan for each
                spannable.setSpan(
                    URLSpan(link.second),
                    startIndexOfLink,
                    startIndexOfLink + link.first.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Search for next occurrence
                startIndexOfLink = spannable.toString().indexOf(
                    link.first,
                    startIndexOfLink + link.first.length
                )
            }
        }
    }

    override fun onAppSelected(name: String) {
        val chooseString = getString(R.string.authorization_permission_choose_account_provider)
        binding.fragmentAuthorizationLegalInfoTextView.text = if (legalText.isEmpty()) {
            getString(
                R.string.authorization_permission_legal_info,
                appIdentifiers[0].name
            ) + getString(R.string.authorization_permission_legal_info_fixed, name, chooseString)
        } else {
            String.format(legalText, name) + getString(R.string.authorization_permission_legal_info_fixed, name, chooseString)
        }
        // Re-Create link to privacy center after change
        binding.fragmentAuthorizationLegalInfoTextView.createLinks(Pair(
                getString(R.string.authorization_permission_privacycenter_match),
                getString(R.string.authorization_permission_privacycenter_link)))
    }
}
