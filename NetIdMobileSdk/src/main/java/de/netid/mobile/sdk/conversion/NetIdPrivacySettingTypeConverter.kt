package de.netid.mobile.sdk.conversion

import de.netid.mobile.sdk.model.NetIdPrivacySettingType
import de.netid.mobile.sdk.model.permission.response.read.v1.NetIdPrivacySettingTypeParseModelV1

object NetIdPrivacySettingTypeConverter {
    fun convert(type: NetIdPrivacySettingTypeParseModelV1): NetIdPrivacySettingType {
        return when (type) {
            NetIdPrivacySettingTypeParseModelV1.IDCONSENT -> NetIdPrivacySettingType.IdConsent
            NetIdPrivacySettingTypeParseModelV1.IAB_TC_STRING -> NetIdPrivacySettingType.IabTcString
            NetIdPrivacySettingTypeParseModelV1.OTHER -> NetIdPrivacySettingType.Other
        }
    }
}
