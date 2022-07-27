package de.netid.mobile.sdk.util

import android.content.pm.PackageManager
import de.netid.mobile.sdk.model.AppIdentifier

class PackageUtil {

    companion object {

        fun getInstalledPackages(appIdentifiers: List<AppIdentifier>, packageManager: PackageManager): List<String> {
            val installedPackageNames = mutableListOf<String>()
            for (item in appIdentifiers) {
                if (isPackageInstalled(item.android.applicationId, packageManager)) {
                    installedPackageNames.add(item.android.applicationId)
                }
            }

            return installedPackageNames
        }

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
