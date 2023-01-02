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

package de.netid.mobile.example

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.DriverAtoms.*
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.netid.mobile.sdk.example.MainActivity
import de.netid.mobile.sdk.example.R
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

    @Before
    fun setup() {
        val activityScenario: ActivityScenario<MainActivity> =
            ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
    }

    fun findInLog(search: String) : Boolean {
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
    fun testButtonStatesAtStartup() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonUserInfo)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonPermissionRead)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonPermissionWrite)).check(matches(isNotEnabled()))
    }

    @Test
    fun testLog() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonInitialize)).perform(click())
        onView(withId(R.id.activityMainLogsTextView)).check(matches(withText(containsString("netID service initialized successfully"))))
    }

    @Test
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
    fun testLoginFlowOkay() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonInitialize)).perform(click())

        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).perform(click())

        onView(withId(android.R.id.button2)).perform(click());
        onView(withId(de.netid.mobile.sdk.R.id.fragmentAuthorizationButtonAgreeAndContinue)).perform(click())

        onView(withText("Weiter")).perform(click())


        onWebView().withElement(findElement(Locator.ID, "proceed")).perform(webClick())


        onWebView().withElement(findElement(Locator.ID, "email")).perform(clearElement())
        onWebView().withElement(findElement(Locator.ID, "email")).perform(webClick())
        onWebView().withElement(findElement(Locator.ID, "email")).perform(webKeys("blubber"))
        onWebView().withElement(findElement(Locator.ID, "submitBtn")).perform(webClick())
        sleep(2000)
    }

    @Test
    fun testPermissionFlowOkay() {
        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonInitialize)).perform(click())

        onView(withId(R.id.activityMainButtonInitialize)).check(matches(isNotEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).check(matches(isEnabled()))
        onView(withId(R.id.activityMainButtonAuthorize)).perform(click())

        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(de.netid.mobile.sdk.R.id.fragmentAuthorizationButtonAgreeAndContinue)).perform(click())

        sleep(5000)
        onWebView().withElement(findElement(Locator.ID, "email")).perform(clearElement())
        onWebView().withElement(findElement(Locator.ID, "email")).perform(webKeys("blubber"))
        sleep(2000)
    }

    @Test
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