// Copyright 2026 European netID Foundation (https://enid.foundation)
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

package de.netid.mobile.sdk.model.permission.response.read.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NetIdPrivacySettingParseModelV2(
    @SerialName("idconsent")
    val idConsent: NetIdPrivacySettingsIdConsentV2? = null,
    @SerialName("iab_tcstring")
    val iabTcString: NetIdPrivacySettingsIabTcString? = null
) {
    override fun toString(): String {
        val format = Json { encodeDefaults = false }
        return format.encodeToString(this)
    }
}
