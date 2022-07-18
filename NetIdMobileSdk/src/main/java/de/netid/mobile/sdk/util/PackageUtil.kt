package de.netid.mobile.sdk.util

import android.content.pm.PackageManager

class PackageUtil {

    companion object {

        fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
            return try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (exception: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}
