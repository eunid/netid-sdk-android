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

package de.netid.mobile.sdk.util

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class TokenUtil {

    companion object {
        private const val claimPermissionManagement = "permission_management"
        private const val accessTokenKey = "access_token"

        private fun decode(token: String): List<String> {
            val parts: Array<String> = token.split(".").toTypedArray()
            val decodedString: MutableList<String> = mutableListOf()
            parts.forEachIndexed { index, part ->
                if (index < 2) {
                    val bytes: ByteArray = Base64.decode(part, Base64.URL_SAFE)
                    decodedString.add(String(bytes, StandardCharsets.UTF_8))
                }
            }
            return decodedString
        }

        fun getPermissionTokenFrom(token: String): String? {
            decode(token)[1].let { permissionClaim ->
                return try {
                    val json = JSONObject(permissionClaim)
                    val permissions: JSONObject? =
                        json.get(claimPermissionManagement) as? JSONObject
                    permissions?.get(accessTokenKey) as? String
                } catch (jse: JSONException) {
                    null
                }
            }
        }

        fun isValidJwtToken(token: String): Boolean {
            return decode(token).size == 3
        }
    }
}