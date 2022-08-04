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
                if (isPackageInstalled(item.android.applicationId, packageManager)) {
                    installedAppIdentifiers.add(item)
                }
            }

            return installedAppIdentifiers
        }

        /**
         * Checks, if a given package name is installed on the current device.
         *
         * @param packageName the package name
         * @param packageManager the [PackageManager] instance with which the check is processed
         *
         * @return `true`, if the application related to the given package name is installed; `false` otherwise
         */
        private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
            return try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (exception: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}
