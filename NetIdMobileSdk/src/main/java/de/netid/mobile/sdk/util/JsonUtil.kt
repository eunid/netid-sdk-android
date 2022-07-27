package de.netid.mobile.sdk.util

import android.content.Context
import android.util.Log
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.model.NetIdAppIdentifiers
import kotlinx.serialization.decodeFromString
import org.json.JSONException
import org.json.JSONObject
import kotlinx.serialization.json.Json
import java.io.IOException

class JsonUtil {

    companion object {

        private const val appIdentifierArrayKey = "netIdAppIdentifiers"

        private fun loadJsonFileFromAssets(filename: String, context: Context): String? {
            var jsonString: String? = null
            try {
                val inputStream = context.assets.open(filename)
                val buffer = ByteArray(inputStream.available())
                inputStream.read(buffer)
                inputStream.close()
                jsonString = String(buffer, Charsets.UTF_8)
            } catch (exception: IOException) {
                exception.printStackTrace()
                Log.e(JsonUtil::class.java.simpleName, "Error while reading file $filename")
            }

            return jsonString
        }

        fun loadAppIdentifiers(filename: String, context: Context): List<AppIdentifier> {
            val appIdentifiers = mutableListOf<AppIdentifier>()

            loadJsonFileFromAssets(filename, context)?.let { jsonString ->
                try {
                    val appIdentifiersJson = Json.decodeFromString<NetIdAppIdentifiers>(jsonString)
                    appIdentifiers.addAll(appIdentifiersJson.netIdAppIdentifiers)
                } catch (exception: JSONException) {
                    exception.printStackTrace()
                    Log.e(JsonUtil::class.java.simpleName, "Error while parsing JSON $jsonString")
                }
            } ?: run {
                Log.e(JsonUtil::class.java.simpleName, "JSON string is empty")
            }

            return appIdentifiers
        }
    }
}
