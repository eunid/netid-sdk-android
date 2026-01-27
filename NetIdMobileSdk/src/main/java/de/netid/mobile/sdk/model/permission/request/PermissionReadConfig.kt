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

package de.netid.mobile.sdk.model.permission.request

import de.netid.mobile.sdk.api.NetIdIdentifierFetchOption
import de.netid.mobile.sdk.constants.WebserviceConstants
import de.netid.mobile.sdk.conversion.permission.response.read.PermissionReadResponseConverter
import de.netid.mobile.sdk.model.permission.response.read.PermissionReadResponse
import de.netid.mobile.sdk.model.permission.response.read.v1.PermissionReadResponseParseModelV1
import de.netid.mobile.sdk.model.permission.response.read.v2.PermissionReadResponseParseModelV2
import kotlinx.serialization.json.Json

data class PermissionReadConfig(
    val queryParameter: PermissionQueryParameter? = null,
    val acceptHeader: String,
    val decodePermissionResponse: (responseBody: String) -> PermissionReadResponse
) {
    companion object {
        fun defaultConfiguration(): PermissionReadConfig = PermissionReadConfig(
            acceptHeader = WebserviceConstants.ACCEPT_HEADER_PERMISSION_READ_AUDIT,
            decodePermissionResponse = { responseBody ->
                decodePermissionReadConfigV1(responseBody)
            }
        )

        fun defaultCollapseSyncConfiguration(): PermissionReadConfig = PermissionReadConfig(
            acceptHeader = WebserviceConstants.ACCEPT_HEADER_PERMISSION_READ,
            decodePermissionResponse = { responseBody ->
                decodePermissionReadConfigV1(responseBody)
            }
        )

        fun tokenBasedConfiguration(identifierFetchOptions: Set<NetIdIdentifierFetchOption>): PermissionReadConfig {
            val permissionQueryParameterValue = identifierFetchOptions.joinToString(",") { element ->
                element.queryString
            }

            return PermissionReadConfig(
                queryParameter = PermissionQueryParameter(
                    key = WebserviceConstants.PERMISSION_READ_QUERY_PARAM_IDENTIFIER_KEY,
                    value = permissionQueryParameterValue
                ),
                acceptHeader = WebserviceConstants.ACCEPT_HEADER_PERMISSION_READ_V2,
                decodePermissionResponse = { responseBody ->
                    decodePermissionReadConfigV2(responseBody)
                }
            )
        }

        private fun decodePermissionReadConfigV1(responseBody: String): PermissionReadResponse {
            var permissionResponse: PermissionReadResponseParseModelV1
            // Unknown JSON claims are ignored, unknown ENUM values mapped to default
            val format = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

            permissionResponse = format.decodeFromString(responseBody)

            return PermissionReadResponseConverter.convert(permissionResponse)
        }

        private fun decodePermissionReadConfigV2(responseBody: String): PermissionReadResponse {
            var permissionResponse: PermissionReadResponseParseModelV2
            // Unknown JSON claims are ignored, unknown ENUM values mapped to default
            val format = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

            permissionResponse = format.decodeFromString(responseBody)

            return PermissionReadResponseConverter.convert(permissionResponse)
        }
    }
}
