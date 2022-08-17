package de.netid.mobile.sdk.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.databinding.FragmentAuthorizationBinding
import de.netid.mobile.sdk.model.AppDetailsAndroid
import de.netid.mobile.sdk.model.AppDetailsIOS
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.ui.adapter.AuthorizationAppListAdapter


class AuthorizationFragment(
    private val listener: AuthorizationFragmentListener,
    private val appIdentifiers: MutableList<AppIdentifier> = mutableListOf(),
    private val authorizationIntent: Intent
) : Fragment() {
    companion object {
        private const val netIdScheme = "scheme"
    }

    private var _binding: FragmentAuthorizationBinding? = null

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
        _binding = FragmentAuthorizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStandardButtons()
        setupAppButtons()
    }

    private fun setupStandardButtons() {
        binding.fragmentAuthorizationButtonAgreeAndContinue.setOnClickListener {
            val adapter = binding.fragmentAuthorizationAppCellContainer.adapter as? AuthorizationAppListAdapter
            if (adapter?.selectedPosition != -1) {
                adapter?.getItem(adapter.selectedPosition)?.android?.applicationId?.let { application ->
                    openApp(application)
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
        val listView: ListView = binding.fragmentAuthorizationAppCellContainer
        val listAdapter = context?.let { AuthorizationAppListAdapter(it, appIdentifiers) }
        listView.adapter = listAdapter
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
}
