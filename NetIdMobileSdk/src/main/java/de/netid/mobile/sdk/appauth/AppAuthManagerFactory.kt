package de.netid.mobile.sdk.appauth

class AppAuthManagerFactory {

    companion object {

        fun createAppAuthManager(): AppAuthManager {
            return AppAuthManagerImpl()
        }
    }
}
