package dev.jdtech.jellyfin

import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dev.jdtech.jellyfin.di.DatabaseModule
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DatabaseModule::class)
class MainActivityTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Before
    fun setUp() {
        hiltRule.inject()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun testMainFlow() {
        launchActivity<MainActivity>().use {
            // Wait for the app to load
            waitForElement(allOf(withId(R.id.edit_text_server_address), isDisplayed()))

            // Connect to demo server
            onView(withId(R.id.edit_text_server_address)).perform(typeText("https://demo.jellyfin.org/stable"), closeSoftKeyboard())
            onView(withId(R.id.button_connect)).perform(click())

            // Connecting to the server
            waitForElement(allOf(withId(R.id.edit_text_username), isDisplayed()))

            // Login
            onView(withId(R.id.edit_text_username)).perform(typeText("demo"), closeSoftKeyboard())
            onView(withId(R.id.button_login)).perform(click())

            // Navigate to My media
            waitForElement(allOf(withText("Continue Watching"), isDisplayed()))
            onView(withId(R.id.mediaFragment)).perform(click())

            // Navigate to movies
            waitForElement(allOf(withText("Movies"), isDisplayed()))
            onView(withText("Movies")).perform(click())

            // Navigate to Battle of the Stars
            waitForElement(allOf(withText("Battle of the Stars"), isDisplayed()))
            onView(withText("Battle of the Stars")).perform(click())

            // Play the movie
            waitForElement(allOf(withId(R.id.play_button), isEnabled()))
            onView(withId(R.id.play_button)).perform(click())

            // Wait for movie to start playing
            waitForElement(allOf(withId(androidx.media3.ui.R.id.exo_buffering), isDisplayed()))
            waitForElement(allOf(withId(androidx.media3.ui.R.id.exo_buffering), not(isDisplayed())))

            // Navigate back
            Espresso.pressBack()
        }
    }
}
