package de.netid.mobile.sdk.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
import de.netid.mobile.sdk.databinding.FragmentAuthorizationBinding
import de.netid.mobile.sdk.model.AppIdentifier

class AuthorizationHardFragment(
    private val listener: AuthorizationFragmentListener,
    private val appIdentifiers: List<AppIdentifier>,
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
            resultLauncher.launch(authorizationIntent)
//            openApp("com.example.auth.app")
        }

        binding.fragmentAuthorizationButtonClose.setOnClickListener {
            listener.onCloseClicked()
        }
    }

    private fun setupAppButtons() {
        appIdentifiers.forEachIndexed { index, appIdentifier ->
            val appButton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle)
            appButton.text = appIdentifier.name
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
                listener.onAppButtonClicked(appIdentifier)
            }

            binding.fragmentAuthorizationButtonContainerLayout.addView(appButton, index)
        }
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
