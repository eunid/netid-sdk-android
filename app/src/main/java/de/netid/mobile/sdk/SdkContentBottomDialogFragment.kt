package de.netid.mobile.sdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.netid.mobile.sdk.databinding.BottomDialogSdkContentBinding

class SdkContentBottomDialogFragment : BottomSheetDialogFragment() {

    var sdkContentFragment: Fragment? = null

    private var _binding: BottomDialogSdkContentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomDialogSdkContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sdkContentFragment?.let { contentFragment ->
            childFragmentManager.beginTransaction().replace(binding.bottomDialogSdkContentContainerLayout.id, contentFragment).commit()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
