package de.netid.mobile.sdk.util

import android.util.Base64
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
                    val bytes: ByteArray = Base64.decode(part, Base64.DEFAULT)
                    decodedString.add(String(bytes, StandardCharsets.UTF_8))
                }
            }
            return decodedString
        }

        fun getPermissionTokenFrom(token: String): String? {
            decode(token)[1].let { permissionClaim ->
                val json = JSONObject(permissionClaim)
                val permissions: JSONObject? = json.get(claimPermissionManagement) as? JSONObject
                return permissions?.get(accessTokenKey) as? String
            }
        }

        fun isValidJwtToken(token: String): Boolean {
            return decode(token).size == 3
        }
    }
}