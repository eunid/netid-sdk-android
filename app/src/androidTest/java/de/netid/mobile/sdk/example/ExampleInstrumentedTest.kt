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

package de.netid.mobile.sdk.example

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.web.webdriver.DriverAtoms.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    // These constants define data for logging into one of the account providers.
    // These value have to be adjusted.
    private val login = "your-mail@account.provider"
    private val password = "superSecretPassword"

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    // Helper function to search for a certain string in the logs.
    private fun findInLog(search: String) : Boolean {
        onView(withId(R.id.activityMainLogsTextView)).check(matches(withText(containsString(search))))
        return true
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("de.netid.mobile.sdk", appContext.packageName)
    }

    @Test
    // All buttons but the first one have to be disabled at start.
    fun testButtonStatesAtStartup() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonUserInfo)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonPermissionRead)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonPermissionWrite)).check(matches(isNotEnabled()))
    }

    @Test
    // Test log functionality.
    fun testLog() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonInitialize)).perform(click())
        onView(withId(R.id.activityMainLogsTextView)).check(matches(withText(containsString("netID service initialized successfully"))))
    }

    @Test
    // Start with a login flow but cancel it before entering the web view.
    fun testLoginFlowCancel() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonInitialize)).perform(click())

        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).perform(click())

        onView(withId(android.R.id.button2)).perform(click());
        onView(withId(de.netid.mobile.sdk.R.id.fragmentAuthorizationButtonClose)).perform(click())
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainLogsTextView)).check(matches(withText(containsString("netID service user canceled authentication in process: Authentication"))))
    }

    @Test
    // Do complete login flow cycle.
    fun testLoginFlowOkay() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonInitialize)).perform(click())
        sleep(2000)

        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).perform(click())

        onView(withId(android.R.id.button2)).perform(click());
        onView(withId(de.netid.mobile.sdk.R.id.fragmentAuthorizationButtonAgreeAndContinue)).perform(click())

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val selector = UiSelector()
        val link = device.findObject(selector.resourceId("edit"))
        if (link.exists()) {
            link.click()
        }

        val email = device.findObject(selector.resourceId("email"))
        email.text = this.login
        device.findObject(selector.resourceId("proceed")).click()

        val password = device.findObject(selector.resourceId("password"))
        password.text = this.password
        device.findObject(selector.resourceId("login-submit")).click()

        device.findObject(selector.resourceId("approve")).click()
        sleep(2000)

        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        assert(findInLog("Access Token:"))

        onView(withId(R.id.activityMainButtonUserInfo)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonUserInfo)).perform(click())
        sleep(2000)
        assert(findInLog("netID service user info - fetch finished successfully"))

        onView(withId(R.id.activityMainButtonPermissionRead)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonPermissionRead)).perform(click())
        sleep(2000)
        assert(findInLog("netID service permission - fetch failed with error"))

        // At the end, we end the session and test if the "Authorisieren" button is enabled again.
        onView(withId(R.id.activityMainButtonEndSession)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonEndSession)).perform(click())
    }

    @Test
    // Do complete permission flow cycle.
    fun testPermissionFlowOkay() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonInitialize)).perform(click())

        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).perform(click())

        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(de.netid.mobile.sdk.R.id.fragmentAuthorizationButtonAgreeAndContinue)).perform(click())

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val selector = UiSelector()
        val link = device.findObject(selector.resourceId("edit"))
        if (link.exists()) {
            link.click()
        }

        val email = device.findObject(selector.resourceId("email"))
        email.text = this.login
        device.findObject(selector.resourceId("proceed")).click()

        val password = device.findObject(selector.resourceId("password"))
        password.text = this.password
        device.findObject(selector.resourceId("login-submit")).click()
        sleep(2000)

        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        assert(findInLog("Access Token:"))

        onView(withId(R.id.activityMainButtonUserInfo)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonUserInfo)).perform(click())
        sleep(2000)
        assert(findInLog("netID service user info - fetch failed"))

        onView(withId(R.id.activityMainButtonPermissionRead)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonPermissionRead)).perform(click())
        sleep(2000)
        assert(findInLog("netID service permission - fetch finished successfully"))

        // At the end, we end the session and test if the "Authorisieren" button is enabled again.
        onView(withId(R.id.activityMainButtonEndSession)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonEndSession)).perform(click())
    }

    @Test
    // Test that extra claims can only be changed before initialisation.
    fun testExtraClaimsOnlyBeforeInitialising() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainCheckBoxShippingAddress)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainCheckBoxBirthdate)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonInitialize)).perform(click())

        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainCheckBoxShippingAddress)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainCheckBoxBirthdate)).check(matches(isNotEnabled()))
    }
}