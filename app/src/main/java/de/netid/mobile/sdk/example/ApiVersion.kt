package de.netid.mobile.sdk.example

enum class ApiVersion(val versionString: String) {
    API_VERSION_1_5("1.5"),
    API_VERSION_1_6("1.6");

    companion object {
        fun forVersionString(versionString: String): ApiVersion {
            return if (versionString == "1.6") {
                API_VERSION_1_6
            } else {
                API_VERSION_1_5
            }
        }
    }
}
