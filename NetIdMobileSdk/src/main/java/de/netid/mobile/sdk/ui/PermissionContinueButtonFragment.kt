package de.netid.mobile.sdk.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.api.NetIdAuthFlow
import de.netid.mobile.sdk.api.NetIdService
import de.netid.mobile.sdk.databinding.PermissionContinueButtonBinding
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.ui.adapter.AuthorizationAppListAdapter
import de.netid.mobile.sdk.ui.adapter.AuthorizationAppListAdapterListener

class PermissionContinueButtonFragment(
    private val listener: AuthorizationFragmentListener,
    private val continueText: String = "",
    private val appIdentifiers: MutableList<AppIdentifier>
): Fragment(), AuthorizationAppListAdapterListener {

    private var _binding: PermissionContinueButtonBinding? = null

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
        _binding = PermissionContinueButtonBinding.inflate(inflater, container, false)

        val listView: ListView = binding.buttonPermissionContinueAppCellContainer
        val listAdapter = context?.let { AuthorizationAppListAdapter(requireContext(), appIdentifiers) }
        listAdapter?.listener = this
        listView.adapter = listAdapter
        listView.visibility = View.VISIBLE

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authorizationIntent = context?.let { it1 ->
            NetIdService.authIntentForFlow(NetIdAuthFlow.Permission,
                it1
            )
        }

        binding.buttonPermissionContinue.setOnClickListener {
            authorizationIntent = NetIdService.authIntentForFlow(NetIdAuthFlow.Permission, it.context)
            var adapter = binding.buttonPermissionContinueAppCellContainer.adapter as? AuthorizationAppListAdapter
            // If we only have one app or the user did not make changes to the default, use the standard one.
            if (adapter == null) adapter = context?.let { AuthorizationAppListAdapter(it, appIdentifiers) }
            if ((adapter != null) && (adapter.selectedPosition != -1) && (appIdentifiers.size != 0)) {
                adapter.getItem(adapter.selectedPosition).let { app ->
                    openApp(app)
                }
            } else {
                resultLauncher.launch(authorizationIntent)
            }
        }

        if (continueText.isNotEmpty()) {
            binding.buttonPermissionContinue.text = continueText
        }
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

    override fun onAppSelected(name: String) {
    }
}