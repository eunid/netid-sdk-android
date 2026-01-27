package de.netid.mobile.sdk.conversion.permission.response.read

import de.netid.mobile.sdk.conversion.NetIdPrivacySettingTypeConverter
import de.netid.mobile.sdk.model.NetIdPrivacySetting
import de.netid.mobile.sdk.model.NetIdPrivacySettingType
import de.netid.mobile.sdk.model.permission.response.read.PermissionReadResponse
import de.netid.mobile.sdk.model.permission.response.read.v1.PermissionReadResponseParseModelV1
import de.netid.mobile.sdk.model.permission.response.read.v2.PermissionReadResponseParseModelV2

object PermissionReadResponseConverter {
    fun convert(readResponse: PermissionReadResponseParseModelV1): PermissionReadResponse {
        val privacySettings: List<NetIdPrivacySetting>? = readResponse.netIdPrivacySettings?.let {
            val privacySettingsList = mutableListOf<NetIdPrivacySetting>()

            it.forEach { privacySetting ->
                privacySettingsList.add(
                    NetIdPrivacySetting(
                        NetIdPrivacySettingTypeConverter.convert(privacySetting.type),
                        privacySetting.status,
                        privacySetting.value,
                        privacySetting.changedAt
                    )
                )
            }

            privacySettingsList
        }

        return PermissionReadResponse(
            readResponse.statusCode,
            readResponse.subjectIdentifiers,
            privacySettings
        )
    }

    fun convert(readResponse: PermissionReadResponseParseModelV2): PermissionReadResponse {
        val privacySettings: List<NetIdPrivacySetting>? = readResponse.netIdPrivacySettings?.let {
            val privacySettingsList = mutableListOf<NetIdPrivacySetting>()

            it.idConsent?.let { idConsent ->
                privacySettingsList.add(
                    NetIdPrivacySetting(
                        type = NetIdPrivacySettingType.IdConsent,
                        status = idConsent.status,
                        changedAt = idConsent.changedAt
                    )
                )
            }

            it.iabTcString?.let { iabTcString ->
                privacySettingsList.add(
                    NetIdPrivacySetting(
                        type = NetIdPrivacySettingType.IabTcString,
                        value = iabTcString.value,
                        changedAt = iabTcString.changedAt
                    )
                )
            }

            privacySettingsList
        }

        return PermissionReadResponse(
            readResponse.statusCode,
            readResponse.subjectIdentifiers,
            privacySettings
        )
    }
}
