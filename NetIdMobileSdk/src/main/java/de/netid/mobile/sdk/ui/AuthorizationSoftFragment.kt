package de.netid.mobile.sdk.ui

import android.app.Activity
import android.content.Intent
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
    private val authorizationIntent: Intent
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

    }

    private fun setupStandardButtons() {
        binding.fragmentAuthorizationButtonAgreeAndContinue.text = getString(R.string.authorization_soft_agree_and_continue_with_net_id).uppercase()
        binding.fragmentAuthorizationButtonAgreeAndContinue.setOnClickListener {
            //TODO reactivate once app2app is working
//            val adapter = binding.fragmentAuthorizationAppCellContainer.adapter as? AuthorizationAppListAdapter
//            if (adapter?.selectedPosition != -1) {
//                adapter?.getItem(adapter.selectedPosition)?.android?.applicationId?.let { application ->
//                    openApp(application)
//                }
//            } else {
                resultLauncher.launch(authorizationIntent)
//            }
        }
        binding.fragmentAuthorizationButtonClose.text = getString(R.string.authorization_soft_close).uppercase()
        binding.fragmentAuthorizationButtonClose.setOnClickListener {
            listener.onCloseClicked()
        }
    }

    private fun setupAppButtons() {
        val netIdString = getString(R.string.authorization_soft_net_id)
        val chooseString = getString(R.string.authorization_soft_choose_partner)
        if (appIdentifiers.size == 1) {
            binding.fragmentAuthorizationLegalInfoTextView.text =
                getString(R.string.authorization_soft_legal_info, appIdentifiers[0].name, "")
            return
        }
        if (appIdentifiers.size >= 1) {
            binding.fragmentAuthorizationLegalInfoTextView.text =
                getString(R.string.authorization_soft_legal_info, appIdentifiers[0].name, chooseString)
        } else {
            binding.fragmentAuthorizationLegalInfoTextView.text =
                getString(R.string.authorization_soft_legal_info, netIdString, chooseString)
        }
        binding.fragmentAuthorizationLegalInfoTextView.makeLinks(
            Pair(chooseString, View.OnClickListener {
                val listView: ListView = binding.fragmentAuthorizationAppCellContainer
                val listAdapter = context?.let { AuthorizationAppListAdapter(it, appIdentifiers) }
                listAdapter?.listener = this
                listView.adapter = listAdapter
            }),
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun openApp(packageName: String) {
        val intent = context?.packageManager?.getLaunchIntentForPackage(packageName)
        intent?.putExtra(netIdScheme, context?.applicationInfo?.packageName)
        intent.let {
            context?.startActivity(intent)
        }
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
            spannableString.setSpan(
                clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        this.movementMethod =
            LinkMovementMethod.getInstance()
        this.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    override fun onAppSelected(name: String) {
        val chooseString = getString(R.string.authorization_soft_choose_partner)
        binding.fragmentAuthorizationLegalInfoTextView.text =
            getString(R.string.authorization_soft_legal_info, name, chooseString)
    }
}
