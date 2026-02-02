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

package de.netid.mobile.sdk.example

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SessionPreferences {
    private const val STORE_NAME = "netIdSdkApp"
    private const val KEY_BACKEND_VERSION = "backendVersion"
    private var sharedPreferences: SharedPreferences? = null

    fun saveBackendVersion(context: Context, version: String) {
        getSharedPreferences(context).edit {
            putString(KEY_BACKEND_VERSION, version)
        }
    }

    fun loadBackendVersion(context: Context, defaultValue: String): String {
        return getSharedPreferences(context).getString(KEY_BACKEND_VERSION, defaultValue) ?: run {
            defaultValue
        }
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return sharedPreferences ?: run {
            val preferences = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
            sharedPreferences = preferences
            preferences
        }
    }
}
