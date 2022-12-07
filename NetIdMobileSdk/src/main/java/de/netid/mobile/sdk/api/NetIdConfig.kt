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

package de.netid.mobile.sdk.api

/**
 * @property logoId References an icon resource by id to be set during permission flow (in the upper left corner of the dialog).
 * @property headlineText Sets the text of the headline (beneath the logo).
 * @property legalText Sets the text of the first part of the legal information text. However, the second part is fixed and can not be set.
 * @property continueText Sets the text of the continue button at the bottom of the dialog.
 */
data class PermissionLayerConfig(
    val logoId: String? = "",
    val headlineText: String? = "",
    val legalText: String? = "",
    val continueText: String? = ""
)

/**
 * @property headlineText Sets the text of the headline (beneath the logo). Only visible, if at least one id app is installed.
 * @property loginText Sets the text of the buttons displayed, if id apps are installed. The name of the app will be displayed as well, if the string is a format sting containing  "%s".
 * @property continueText Sets the text of the continue button at the bottom of the dialog. Only visible, if there is no id app installed.
 */
data class LoginLayerConfig(
    val headlineText: String? = "",
    val loginText: String? = "",
    val continueText: String? = ""
)

/**
 * @property clientId The client id of this application. You need to retrieve it from the netID developer portal.
 * @property redirectUri Redirect URI for your application.  You need to retrieve it from the netID developer portal.
 * @property claims Additional claims to set. This needs to be a JSON string,but can be null.
 * @property promptWeb Additional value for parameter `prompt` that will be used during app2web-flow only.
 * @property permissionLayerConfig Optional configuration for strings and logo to display in the permission layer.
 * @property loginLayerConfig Optional configuration for strings to display in the login layer.
 */
data class NetIdConfig(
    val clientId: String,
    val redirectUri: String,
    val claims: String? = null,
    val promptWeb: String? = null,
    val permissionLayerConfig: PermissionLayerConfig? = null,
    val loginLayerConfig: LoginLayerConfig? = null
)
