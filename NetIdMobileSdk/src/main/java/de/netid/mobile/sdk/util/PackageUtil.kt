package de.netid.mobile.sdk.util

import android.content.pm.PackageManager

class PackageUtil {

    companion object {

        fun getInstalledPackages(packageNames: List<String>, packageManager: PackageManager): List<String> {
            val installedPackageNames = mutableListOf<String>()
            for (packageName in packageNames) {
                if (isPackageInstalled(packageName, packageManager)) {
                    installedPackageNames.add(packageName)
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
