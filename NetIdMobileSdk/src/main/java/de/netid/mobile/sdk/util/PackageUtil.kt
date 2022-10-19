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

import android.content.pm.PackageManager
import de.netid.mobile.sdk.model.AppIdentifier

/**
 * Provides functionalities for checks related to a [PackageManager].
 */
class PackageUtil {

    companion object {

        /**
         * Queries the given list of [AppIdentifier] elements and checks if the related apps are installed on the current device.
         *
         * @param appIdentifiers a list of [AppIdentifier] elements representing specific applications
         * @param packageManager the [PackageManager] instance with which the checks are processed
         *
         * @return a list of [AppIdentifier] elements representing installed applications
         */
        fun getInstalledPackages(appIdentifiers: List<AppIdentifier>, packageManager: PackageManager): List<AppIdentifier> {
            val installedAppIdentifiers = mutableListOf<AppIdentifier>()
            for (item in appIdentifiers) {
                if (isPackageInstalled(item.android.applicationId, item.android.activityFilter, packageManager)) {
                    installedAppIdentifiers.add(item)
                }
            }

            return installedAppIdentifiers
        }

        /**
         * Checks, if a given package name with a certain activity is installed on the current device.
         *
         * @param packageName the package name
         * @param activityName the activity name
         * @param packageManager the [PackageManager] instance with which the check is processed
         *
         * @return `true`, if the application related to the given package name is installed; `false` otherwise
         */
        private fun isPackageInstalled(packageName: String, activityName: String, packageManager: PackageManager): Boolean {
            return try {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                // Filter for specific activity
                if (info.activities != null) {
                    info.activities.forEach {
                        if (it.name.equals(activityName)) {
                            return@isPackageInstalled true
                        }
                    }
                }
                true
            } catch (exception: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}
