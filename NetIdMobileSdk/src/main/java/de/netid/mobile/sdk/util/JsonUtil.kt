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

import android.content.Context
import android.util.Log
import de.netid.mobile.sdk.model.AppIdentifier
import de.netid.mobile.sdk.model.NetIdAppIdentifiers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONException
import java.io.IOException

/**
 * Provides functionalities to load and process JSON files.
 */
class JsonUtil {

    companion object {

        /**
         * Loads a specific JSON file from the assets.
         *
         * @param filename the filename of the JSON file to load from the assets
         * @param context a context with which the assets are accessed
         *
         * @return A string containing the content of the loaded JSON file.
         * If the file does not exist or could not be loaded, the returned string is `null`.
         */
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

        /**
         * Loads a JSON file containing app identifier information from the assets.
         *
         * @param filename the filename of the JSON file containing app identifier information
         * @param context a context with which the assets are accessed
         *
         * @return a list containing [AppIdentifier] elements representing the loaded information from the JSON file
         */
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
