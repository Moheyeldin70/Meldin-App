package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    val dbId = context.getString(R.string.firestore_database_id)
    assertEquals("Meldin", appName)
    assertTrue(dbId.isNotBlank())
  }

  @Test
  fun `main activity launches and transitions to sign in without crashing`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("skip_splash_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
  }
}
