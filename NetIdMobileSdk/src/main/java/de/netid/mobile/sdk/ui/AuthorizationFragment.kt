package de.netid.mobile.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.databinding.FragmentAuthorizationBinding
import de.netid.mobile.sdk.model.AppIdentifier

class AuthorizationFragment(
    private val listener: AuthorizationFragmentListener,
    private val appIdentifiers: List<AppIdentifier>
) : Fragment() {

    private var _binding: FragmentAuthorizationBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

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
    }

    private fun setupStandardButtons() {
        binding.fragmentAuthorizationButtonAgreeAndContinue.setOnClickListener {
            listener.onAgreeAndContinueWithNetIdClicked()
        }

        binding.fragmentAuthorizationButtonClose.setOnClickListener {
            listener.onCloseClicked()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
