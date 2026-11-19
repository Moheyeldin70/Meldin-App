package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.EXPECTED_DEBUG_SHA1
import com.example.data.auth.formatSignInFailureMessage
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
    val webClientId = context.getString(R.string.default_web_client_id)
    assertEquals("Meldin", appName)
    assertTrue(dbId.isNotBlank())
    assertTrue(webClientId.isNotBlank())
  }

  @Test
  fun `sign in failure formatting highlights sha1 mismatch even for NoCredentialException`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val mismatchMsg = formatSignInFailureMessage(
      context = context,
      e = NoCredentialException("No credentials available"),
      actualSha1Override = "11:22:33:44:55"
    )
    assertTrue(mismatchMsg.contains("11:22:33:44:55"))
    assertTrue(mismatchMsg.contains(EXPECTED_DEBUG_SHA1))

    val reauthMatchedMsg = formatSignInFailureMessage(
      context = context,
      e = GetCredentialCancellationException("[16] Account reauth failed."),
      actualSha1Override = EXPECTED_DEBUG_SHA1
    )
    assertTrue(reauthMatchedMsg.contains("[16] Account reauth failed."))
    assertTrue(reauthMatchedMsg.contains(EXPECTED_DEBUG_SHA1))
  }

  @Test
  fun `main activity launches and transitions to sign in without crashing`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("skip_splash_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithTag("google_sign_in_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
  }
}
