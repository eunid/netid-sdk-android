package de.netid.mobile.sdk.conversion.permission.response.read

import de.netid.mobile.sdk.model.NetIdPrivacySetting
import de.netid.mobile.sdk.model.NetIdPrivacySettingType
import de.netid.mobile.sdk.model.permission.response.read.PermissionReadResponse
import de.netid.mobile.sdk.model.permission.response.read.parse.PermissionReadResponseParseModel

object PermissionReadResponseConverter {
    fun convert(readResponse: PermissionReadResponseParseModel): PermissionReadResponse {
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
