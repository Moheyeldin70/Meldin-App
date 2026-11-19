package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.ContentType
import com.example.data.model.Post
import com.example.data.model.PrivateItem
import com.example.data.model.SavedPost
import com.example.data.model.UiState
import com.example.localization.LocalMeldinStrings
import com.example.localization.getStringsForLanguage
import com.example.ui.theme.MeldinTheme
import com.example.ui.theme.ThemePreference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MeldinShellRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `authenticated shell renders all five bottom navigation tabs and account screen`() {
        val samplePosts = listOf(
            Post(
                id = "lv1",
                authorId = "u1",
                authorName = "Meldin Creator",
                type = ContentType.LONG_VIDEO.name,
                title = "Long Form Film",
                description = "Documentary",
                category = "Design",
                durationSeconds = 300L
            ),
            Post(
                id = "sv1",
                authorId = "u1",
                authorName = "Meldin Creator",
                type = ContentType.SHORT_VIDEO.name,
                title = "Short Reel",
                description = "Quick clip",
                category = "Cinema",
                durationSeconds = 30L
            ),
            Post(
                id = "img1",
                authorId = "u1",
                authorName = "Meldin Creator",
                type = ContentType.IMAGE.name,
                title = "Warm Architecture",
                description = "Photo",
                category = "Photo"
            ),
            Post(
                id = "art1",
                authorId = "u1",
                authorName = "Meldin Creator",
                type = ContentType.ARTICLE.name,
                title = "Craft Essay",
                description = "Essay",
                bodyText = "Full article text",
                category = "Editorial"
            )
        )
        val savedPosts = listOf(
            SavedPost(
                id = "lv1",
                postId = "lv1",
                userId = "u1",
                type = ContentType.LONG_VIDEO.name,
                title = "Long Form Film",
                authorName = "Meldin Creator"
            )
        )
        val privateItems = listOf(
            PrivateItem(
                id = "p1",
                userId = "u1",
                type = ContentType.ARTICLE.name,
                title = "Private Draft",
                description = "Notes",
                category = "Personal"
            )
        )

        composeTestRule.setContent {
            val strings = getStringsForLanguage("en")
            CompositionLocalProvider(LocalMeldinStrings provides strings) {
                MeldinTheme(themePreference = ThemePreference.LIGHT) {
                    MeldinAuthenticatedScaffold(
                        uid = "u1",
                        displayName = "Test Member",
                        userEmail = "member@example.com",
                        photoUrl = null,
                        isAdmin = false,
                        isAdminUnlocked = false,
                        adminConfig = null,
                        postsState = UiState.Success(samplePosts),
                        savedPostsState = UiState.Success(savedPosts),
                        privateItemsState = UiState.Success(privateItems),
                        searchQuery = "",
                        selectedCategory = null,
                        isBusy = false,
                        activePostId = null,
                        activeCommentsState = UiState.Success(emptyList()),
                        isActivePostLiked = false,
                        themePreference = ThemePreference.LIGHT,
                        currentLanguageCode = "en",
                        onChangeTheme = {},
                        onSelectLanguage = {},
                        onSearchQueryChange = {},
                        onSelectCategory = {},
                        onOpenPost = {},
                        onClosePost = {},
                        onToggleSavePost = { _, _ -> },
                        onToggleLikeActivePost = {},
                        onAddComment = { _, _ -> },
                        onDeleteComment = { _, _ -> },
                        onCreatePrivateItem = { _, _, _, _, _, _, _, _, _ -> },
                        onUpdatePrivateItem = { _, _, _, _, _, _, _, _, _, _ -> },
                        onDeletePrivateItem = {},
                        onVerifyAdminPassword = { _, _ -> },
                        onSetAdminPassword = { _, _, _ -> },
                        onLockAdmin = {},
                        onPublishPost = { _, _, _, _, _, _, _, _, _, _ -> },
                        onUpdatePost = { _, _, _, _, _, _, _, _, _, _ -> },
                        onDeletePost = {},
                        onPublishStarterPack = {},
                        onAccountSwitched = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_${MeldinRoutes.ROUTE_LONG_VIDEOS}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("post_card_lv1").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_${MeldinRoutes.ROUTE_SHORT_VIDEOS}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("short_reel_sv1").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_${MeldinRoutes.ROUTE_IMAGES}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("image_card_img1").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_${MeldinRoutes.ROUTE_ARTICLES}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("article_card_art1").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_${MeldinRoutes.ROUTE_LIBRARY}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_saved_tab").assertIsDisplayed()

        composeTestRule.onNodeWithTag("library_account_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_admin_dashboard").assertIsDisplayed()

        composeTestRule.onNodeWithTag("account_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_saved_tab").assertIsDisplayed()
    }
}
