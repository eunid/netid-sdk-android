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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.databinding.FragmentAuthorizationSoftBinding
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.ui.adapter.AuthorizationAppListAdapter
import de.netid.mobile.sdk.ui.adapter.AuthorizationAppListAdapterListener


class AuthorizationSoftFragment(
    private val listener: AuthorizationFragmentListener,
    private val appIdentifiers: MutableList<AppIdentifier> = mutableListOf(),
    private val authorizationIntent: Intent,
    private val logoId: String = "",
    private val headlineText: String = "",
    private val legalText: String = "",
    private val continueText: String = ""
) : Fragment(), AuthorizationAppListAdapterListener {
    companion object {
        private const val netIdScheme = "scheme"
    }

    private var _binding: FragmentAuthorizationSoftBinding? = null

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
        _binding = FragmentAuthorizationSoftBinding.inflate(inflater, container, false)
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
        binding.fragmentAuthorizationButtonAgreeAndContinue.text = getString(R.string.authorization_soft_agree_and_continue_with_net_id).uppercase()
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
        val netIdString = getString(R.string.authorization_soft_net_id)
        val chooseString = getString(R.string.authorization_soft_choose_partner)
        when (appIdentifiers.size) {
            0 -> binding.fragmentAuthorizationLegalInfoTextView.text = if (legalText.isEmpty()) {
                getString(R.string.authorization_soft_legal_info, netIdString) + getString(R.string.authorization_soft_legal_info_fixed, netIdString, "")
            } else {
                String.format(legalText, netIdString) + getString(R.string.authorization_soft_legal_info_fixed, netIdString, "")
            }
            1 -> binding.fragmentAuthorizationLegalInfoTextView.text = if (legalText.isEmpty()) {
                getString(R.string.authorization_soft_legal_info, appIdentifiers[0].name) + getString(R.string.authorization_soft_legal_info_fixed, appIdentifiers[0].name, "")
            } else {
                String.format(legalText, appIdentifiers[0].name) + getString(R.string.authorization_soft_legal_info_fixed, appIdentifiers[0].name, "")
            }
            else -> {
                binding.fragmentAuthorizationLegalInfoTextView.text = if (legalText.isEmpty()) {
                    getString(
                        R.string.authorization_soft_legal_info,
                        appIdentifiers[0].name
                    ) + getString(R.string.authorization_soft_legal_info_fixed, appIdentifiers[0].name, chooseString)
                } else {
                    String.format(legalText, appIdentifiers[0].name) + getString(R.string.authorization_soft_legal_info_fixed, appIdentifiers[0].name, chooseString)
                }
                binding.fragmentAuthorizationLegalInfoTextView.makeLinks(
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
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun openApp(verifiedAppLink: String) {
        val authIntent = authorizationIntent.extras?.get("authIntent")as Intent
        val authUri = authIntent.data as Uri
        val uri = authUri.toString().replaceBefore("?", verifiedAppLink)
        //TODO Works but maybe there are more elegant ways
        authorizationIntent.extras?.apply {
            val i = getParcelable<Intent>("authIntent") ?: return@apply
            //TODO should be set to app package
            i.setPackage(null)
            i.data = Uri.parse(uri)
            putParcelable("authIntent", i)
        }
        resultLauncher.launch(authorizationIntent)
    }

    private fun TextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
        val spannableString = SpannableString(this.text)
        var startIndexOfLink = -1
        for (link in links) {
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
            startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)
            // We only need to add the link to the text, if we have more than one id app installed.
            // Otherwise, we leave the text as it is.
            if (startIndexOfLink != -1) {
                spannableString.setSpan(
                    clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        this.movementMethod =
            LinkMovementMethod.getInstance()
        this.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    override fun onAppSelected(name: String) {
        val chooseString = getString(R.string.authorization_soft_choose_partner)
        binding.fragmentAuthorizationLegalInfoTextView.text = if (legalText.isEmpty()) {
            getString(
                R.string.authorization_soft_legal_info,
                appIdentifiers[0].name
            ) + getString(R.string.authorization_soft_legal_info_fixed, appIdentifiers[0].name, chooseString)
        } else {
            String.format(legalText, appIdentifiers[0].name) + getString(R.string.authorization_soft_legal_info_fixed, appIdentifiers[0].name, chooseString)
        }
    }
}
