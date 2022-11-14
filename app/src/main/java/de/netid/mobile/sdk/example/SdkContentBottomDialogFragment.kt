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

package de.netid.mobile.sdk.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.netid.mobile.sdk.api.NetIdService
import de.netid.mobile.sdk.example.databinding.BottomDialogSdkContentBinding

class SdkContentBottomDialogFragment : BottomSheetDialogFragment() {

    var sdkContentFragment: Fragment? = null

    private var _binding: BottomDialogSdkContentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var isClosed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomDialogSdkContentBinding.inflate(inflater, container, false)
        isClosed = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isClosed = false

        sdkContentFragment?.let { contentFragment ->
            childFragmentManager.beginTransaction().replace(binding.bottomDialogSdkContentContainerLayout.id, contentFragment).commit()
        }
    }

    override fun onDestroyView() {
        _binding = null
        if (!isClosed)
            NetIdService.onCloseClicked()
        isClosed = true
        super.onDestroyView()
    }

    override fun dismiss() {
        super.dismissAllowingStateLoss()
        isClosed = true
    }
}
