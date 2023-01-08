package de.netid.mobile.sdk.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import de.netid.mobile.sdk.R
import de.netid.mobile.sdk.api.NetIdService
import de.netid.mobile.sdk.databinding.PermissionAppButtonBinding
import de.netid.mobile.sdk.model.AppIdentifier

class PermissionAppButtonFragment(
    private val index: Int,
    private val appIdentifier: AppIdentifier
): Fragment() {

    private var _binding: PermissionAppButtonBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PermissionAppButtonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resourceId: Int = resources.getIdentifier(appIdentifier.icon, "drawable", context?.packageName)

        binding.buttonPermissionAppImageView.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                resourceId,
                null
            )
        )
        binding.buttonPermissionAppTextView.text = "XXX - " + String.format(getString(R.string.authorization_login_continue_button), appIdentifier.name).uppercase()
        binding.buttonPermissionApp.setOnClickListener {
            NetIdService.setAppSelection(index)
            binding.buttonPermissionAppRadioButton.toggle()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}