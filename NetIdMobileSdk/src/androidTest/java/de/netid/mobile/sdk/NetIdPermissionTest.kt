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

package de.netid.mobile.sdk

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import de.netid.mobile.sdk.api.*
import de.netid.mobile.sdk.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NetIdPermissionTest: NetIdServiceListener {

    companion object {
        private const val clientId = "082531ba-1b22-4381-81b1-64add4b85b8a"
        private const val host = "broker.netid.de"
        private const val redirectUri = "https://netid-sdk-web.letsdev.de/redirect"
        private const val claims = "{\"userinfo\":{\"email\": {\"essential\": true}, \"email_verified\": {\"essential\": true}}}"

        private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Before
    public fun setup() {
        assertEquals("de.netid.mobile.sdk.test", appContext.packageName)
        val netIdConfig = NetIdConfig(host, clientId, redirectUri, claims)
        NetIdService.addListener(this)

        NetIdService.initialize(netIdConfig, appContext)
    }

    // Don't do all initialization steps, so this must failed as UNAUTHORIZED
    @Test
    fun fetchPermissionsShouldFail() {
        NetIdService.fetchPermissions(appContext)
    }

    @Test
    fun fetchPermissions() {
/*        val continueButton = NetIdService.continueButtonPermissionFlow(this, NetIdAuthFlow.Permission)
        continueButton.performClick()*/
        val appDetailsIOS = AppDetailsIOS("", "", "")
        val appDetailsAndroid = AppDetailsAndroid("de.web.mobile.android.mail", "https://sso.web.de/authorize-app2app", "de.web.mobile.android.mail.NetIdActivityV1")
        val appIdentifier = AppIdentifier(0, "WEB.DE", "", "", "", "", appDetailsIOS, appDetailsAndroid)
        NetIdService.onAppButtonClicked(appIdentifier)

        NetIdService.fetchPermissions(appContext)
    }

    
    // Don't do all initialization steps, so this must failed as UNAUTHORIZED
    @Test
    fun updatePermissionsShouldFail() {
        val permission = NetIdPermissionUpdate(
            "VALID",
            "CPdfZIAPdfZIACnABCDECbCkAP_AAAAAAAYgIzJd9D7dbXFDefx_SPt0OYwW0NBXCuQCChSAA2AFVAOQcLQA02EaMATAhiACEQIAolIBAAEEHAFEAECQQIAEAAHsAgSEhAAKIAJEEBEQAAIQAAoKAAAAAAAIgAABoASAmBiQS5bmRUCAOIAQRgBIgggBCIADAgMBBEAIABgIAIIIgSgAAQAAAKIAAAAAARAAAASGgFABcAEMAPwAgoBaQEiAJ2AUiAxgBnwqASAEMAJgAXABHAEcALSAkEBeYDPh0EIABYAFQAMgAcgA-AEAALgAZAA0AB4AD6AIYAigBMACfAFwAXQAxABmADeAHMAPwAhgBLACYAE0AKMAUoAsQBbgDDAGiAPaAfgB-gEDAIoARaAjgCOgEpALEAWmAuYC6gF5AMUAbQA3ABxADnAHUAPQAi8BIICRAE7AKHAXmAwYBjADJAGVAMsAZmAz4BrADiwHjgPrAg0BDkhAbAAWABkAFwAQwAmABcADEAGYAN4AjgBSgCxAIoARwAlIBaQC5gGKANoAc4A6gB6AEggJEAScAz4B45KBAAAgABYAGQAOAAfAB4AEQAJgAXAAxABmADaAIYARwAowBSgC3AH4ARwAk4BaQC6gGKANwAdQBF4CRAF5gMsAZ8A1gCGoSBeAAgABYAFQAMgAcgA8AEAAMgAaAA8gCGAIoATAAngBvADmAH4AQgAhgBHACWAE0AKUAW4AwwB7QD8AP0AgYBFICNAI4ASkAuYBigDaAG4AOIAegBIgCdgFDgKRAXmAwYBkgDPoGsAayA4IB44EOREAYAQwA_AEiAJ2AUiAz4ZAHACGAEwARwBHAEnALzAZ8UgXAALAAqABkADkAHwAgABkADQAHkAQwBFACYAE8AKQAYgAzABzAD8AIYAUYApQBYgC3AGjAPwA_QCLQEcAR0AlIBcwC8gGKANoAbgA9ACLwEiAJOATsAocBeYDGAGSAMsAZ9A1gDWQHBAPHAhm.f_gAAAAAAsgA"
        )
        val permissionOnlyConsent = NetIdPermissionUpdate(
            "INVALID"
        )
        val permissionOnlyIabTc = NetIdPermissionUpdate(null, "CPdfZIAPdfZIACnABCDECbCkAP_AAAAAAAYgIzJd9D7dbXFDefx_SPt0OYwW0NBXCuQCChSAA2AFVAOQcLQA02EaMATAhiACEQIAolIBAAEEHAFEAECQQIAEAAHsAgSEhAAKIAJEEBEQAAIQAAoKAAAAAAAIgAABoASAmBiQS5bmRUCAOIAQRgBIgggBCIADAgMBBEAIABgIAIIIgSgAAQAAAKIAAAAAARAAAASGgFABcAEMAPwAgoBaQEiAJ2AUiAxgBnwqASAEMAJgAXABHAEcALSAkEBeYDPh0EIABYAFQAMgAcgA-AEAALgAZAA0AB4AD6AIYAigBMACfAFwAXQAxABmADeAHMAPwAhgBLACYAE0AKMAUoAsQBbgDDAGiAPaAfgB-gEDAIoARaAjgCOgEpALEAWmAuYC6gF5AMUAbQA3ABxADnAHUAPQAi8BIICRAE7AKHAXmAwYBjADJAGVAMsAZmAz4BrADiwHjgPrAg0BDkhAbAAWABkAFwAQwAmABcADEAGYAN4AjgBSgCxAIoARwAlIBaQC5gGKANoAc4A6gB6AEggJEAScAz4B45KBAAAgABYAGQAOAAfAB4AEQAJgAXAAxABmADaAIYARwAowBSgC3AH4ARwAk4BaQC6gGKANwAdQBF4CRAF5gMsAZ8A1gCGoSBeAAgABYAFQAMgAcgA8AEAAMgAaAA8gCGAIoATAAngBvADmAH4AQgAhgBHACWAE0AKUAW4AwwB7QD8AP0AgYBFICNAI4ASkAuYBigDaAG4AOIAegBIgCdgFDgKRAXmAwYBkgDPoGsAayA4IB44EOREAYAQwA_AEiAJ2AUiAz4ZAHACGAEwARwBHAEnALzAZ8UgXAALAAqABkADkAHwAgABkADQAHkAQwBFACYAE8AKQAYgAzABzAD8AIYAUYApQBYgC3AGjAPwA_QCLQEcAR0AlIBcwC8gGKANoAbgA9ACLwEiAJOATsAocBeYDGAGSAMsAZ9A1gDWQHBAPHAhm.f_gAAAAAAsgA"
        )

        NetIdService.updatePermission(appContext, permission)
    }

    // Listener functions
    override fun onInitializationFinishedWithError(error: NetIdError?) {
        assertEquals(error, null)
    }

    override fun onAuthenticationFinished(accessToken: String) {
        TODO("Not yet implemented")
    }

    override fun onAuthenticationFinishedWithError(error: NetIdError) {
        TODO("Not yet implemented")
    }

    override fun onUserInfoFinished(userInfo: UserInfo) {
        TODO("Not yet implemented")
    }

    override fun onUserInfoFetchedWithError(error: NetIdError) {
        TODO("Not yet implemented")
    }

    override fun onSessionEnd() {
        TODO("Not yet implemented")
    }

    override fun onEncounteredNetworkError(error: NetIdError) {
        TODO("Not yet implemented")
    }

    override fun onAuthenticationCanceled(error: NetIdError) {
        TODO("Not yet implemented")
    }

    override fun onPermissionUpdateFinishedWithError(error: NetIdError) {
        assertEquals(error.process, NetIdErrorProcess.PermissionWrite)
        assertEquals(error.code, NetIdErrorCode.UnauthorizedClient)
    }

    override fun onPermissionFetchFinishedWithError(error: NetIdError) {
        assertEquals(error.process, NetIdErrorProcess.PermissionRead)
        assertEquals(error.code, NetIdErrorCode.UnauthorizedClient)
    }

    override fun onPermissionFetchFinished(permissions: Permissions) {
        TODO("Not yet implemented")
    }

    override fun onPermissionUpdateFinished() {
        TODO("Not yet implemented")
    }

    override fun onTransmittedInvalidToken() {
        TODO("Not yet implemented")
    }

}