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

package de.netid.mobile.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val sub: String,
    @SerialName("given_name")
    val givenName: String,
    @SerialName("family_name")
    val familyName: String,
    val birthdate: String,
    @SerialName("email_verified")
    val emailVerified: Boolean = false,
    val address: Address? = null,
    @SerialName("shipping_address")
    val shippingAddress: ShippingAddress? = null,
    val gender: String = "",
    val email: String = "",
    ) {
    override fun toString(): String {
        return "UserInfo(sub='$sub', givenName='$givenName', familyName='$familyName', birthdate='$birthdate', emailVerified=$emailVerified, address=$address, shippingAddress=$shippingAddress, gender='$gender', email='$email')"
    }
}