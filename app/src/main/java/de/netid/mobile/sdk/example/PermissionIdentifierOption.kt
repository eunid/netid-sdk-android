package de.netid.mobile.sdk.example

enum class PermissionIdentifierOption(val optionString: String) {
    Default("Default"),
    AllIdentifiers("All Identifiers");

    companion object {
        fun forOptionString(optionString: String): PermissionIdentifierOption {
            return if (optionString == "Default") {
                Default
            } else {
                AllIdentifiers
            }
        }
    }
}
