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

import de.netid.mobile.sdk.api.NetIdConfig
import org.junit.Test

import org.junit.Assert.*

class NetIdConfigTest {
    companion object {
        private const val clientId = "ec54097f-83f6-4bb1-86f3-f7c584c649cd"
        private const val redirectUri = "https://eunid.github.io/redirectApp"
        private const val claims = "{\"userinfo\":{\"email\": {\"essential\": true}, \"email_verified\": {\"essential\": true}}}"
    }

    @Test
    fun initializeNetIdConfig() {
        val netIdConfig = NetIdConfig(clientId, redirectUri, claims)
        assertEquals(netIdConfig.clientId, clientId)
        assertEquals(netIdConfig.redirectUri, redirectUri)
        assertEquals(netIdConfig.claims, claims)
    }

}